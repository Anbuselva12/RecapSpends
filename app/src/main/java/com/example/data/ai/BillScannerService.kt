package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.ScannedBillDraft
import com.example.data.model.ScannedBillItem
import com.example.data.model.ScannedSubItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.InputStream

private const val TAG = "BillScannerService"

class BillScannerService {

    private val billSchema = Schema.obj(
        properties = mapOf(
            "merchant" to Schema.string(description = "Shop, garage, or utility provider name"),
            "gstin" to Schema.string(description = "15-char Indian GSTIN if present, else empty string"),
            "billNumber" to Schema.string(description = "Invoice or receipt number"),
            "confidencePercent" to Schema.integer(description = "Overall extraction confidence 0 to 100"),
            "suggestedCategoryId" to Schema.enumeration(
                values = listOf("veh", "home", "groc", "util", "subs", "hlth", "biz", "food", "fin"),
                description = "Best matching category ID"
            ),
            "suggestedSubcategory" to Schema.string(description = "Best matching subcategory such as Service, Fuel, Groceries, EB bill"),
            "isInterStateIgst" to Schema.boolean(description = "True if IGST applies instead of CGST+SGST"),
            "items" to Schema.array(
                description = "Extracted line items",
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Item or service description"),
                        "qty" to Schema.double(description = "Quantity"),
                        "unit" to Schema.string(description = "Unit e.g. L, pcs, hrs, kg"),
                        "rate" to Schema.double(description = "Per-unit rate before GST in INR"),
                        "discount" to Schema.double(description = "Line discount in INR"),
                        "hsn" to Schema.string(description = "HSN or SAC code if present"),
                        "gstRate" to Schema.double(description = "GST percentage e.g. 0, 5, 12, 18, 28"),
                        "mappedServiceItemId" to Schema.string(
                            description = "Matching vehicle service key if vehicle service: oil, oilf, chain, lube, align, gen, air, brake, ins, tyre, ac, batt, or empty string",
                            nullable = true
                        ),
                        "lowConfidenceNote" to Schema.string(
                            description = "Warning message if a handwritten rate or blurry figure needs review, else empty string",
                            nullable = true
                        ),
                        "subItems" to Schema.array(
                            description = "Optional kit breakdown sub-items (e.g. chain kit split into chain, front sprocket, rear sprocket)",
                            items = Schema.obj(
                                properties = mapOf(
                                    "name" to Schema.string(),
                                    "qty" to Schema.double(),
                                    "rate" to Schema.double()
                                )
                            )
                        )
                    ),
                    optionalProperties = listOf("mappedServiceItemId", "lowConfidenceNote", "subItems")
                )
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = billSchema
            }
        )
    }

    suspend fun extractBillFromBitmap(bitmap: Bitmap): ScannedBillDraft {
        val scaled = scaleBitmapForPayload(bitmap)
        val promptText = """
            You are an Indian GST receipt & bill extraction engine for a personal finance app.
            Extract all merchant details, GSTIN, bill number, and every line item accurately.
            - If a line is a bundled kit (like a "Chain & sprocket kit"), split it into subItems when itemized.
            - Match HSN/SAC codes and GST rates (0, 5, 12, 18, 28).
            - If the bill is for vehicle servicing/repairs, set suggestedCategoryId="veh", suggestedSubcategory="Service",
              and map each line to mappedServiceItemId from: oil, oilf, chain, lube, align, gen, air, brake, ins, tyre, ac, batt.
            - Flag any handwritten, smudged, or uncertain rate in lowConfidenceNote.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(scaled)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
            parseScannedBillJson(rawJson)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Bill extraction timed out", e)
            throw IllegalStateException("Bill scan timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI bill extraction failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to scan bill with AI.", e)
        }
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Unable to open PDF file.")
            pfd.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount == 0) throw IllegalStateException("PDF has no pages.")
                    renderer.openPage(0).use { page ->
                        val bmp = Bitmap.createBitmap(
                            (page.width * 2).coerceAtMost(1600),
                            (page.height * 2).coerceAtMost(2200),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        return@withContext bmp
                    }
                }
            }
        } else {
            val stream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to read selected image.")
            stream.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
                    ?: throw IllegalStateException("Unsupported or corrupted image file.")
            }
        }
    }

    fun renderSampleGarageInvoiceBitmap(): Bitmap {
        val width = 900
        val height = 1220
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#FAF8F2"))

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 25f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            strokeWidth = 2f
        }

        var y = 76f
        canvas.drawText("SRI VEL TWO-WHEELER CARE", 60f, y, titlePaint); y += 40f
        canvas.drawText("GSTIN: 33ABCDE1234F1Z5", 60f, y, bodyPaint); y += 34f
        canvas.drawText("Bill No: INV-20931   Date: 12 Nov 2026", 60f, y, bodyPaint); y += 34f
        canvas.drawText("Vehicle: Classic 350 (TN 99 AB 4821)", 60f, y, bodyPaint); y += 30f
        canvas.drawLine(60f, y, width - 60f, y, linePaint); y += 40f

        canvas.drawText("1. Engine oil 10W-30 (1 L)  HSN 2710  GST 18%", 60f, y, bodyPaint); y += 32f
        canvas.drawText("   2 x 540.00 = 1080.00", 60f, y, bodyPaint); y += 42f

        canvas.drawText("2. Oil filter               HSN 8421  GST 18%", 60f, y, bodyPaint); y += 32f
        canvas.drawText("   1 x 285.00 = 285.00", 60f, y, bodyPaint); y += 42f

        canvas.drawText("3. Chain & sprocket kit     HSN 8714  GST 18%", 60f, y, bodyPaint); y += 32f
        canvas.drawText("   - Drive chain 520        1 x 1450.00", 60f, y, bodyPaint); y += 30f
        canvas.drawText("   - Front sprocket         1 x 420.00", 60f, y, bodyPaint); y += 30f
        canvas.drawText("   - Rear sprocket          1 x 480.00", 60f, y, bodyPaint); y += 42f

        canvas.drawText("4. Wheel alignment & bal.   HSN 9987  GST 18%", 60f, y, bodyPaint); y += 32f
        canvas.drawText("   (handwritten) 1 x 450.00 = 450.00", 60f, y, handPaint); y += 42f

        canvas.drawText("5. General service labour   SAC 9987  GST 18%", 60f, y, bodyPaint); y += 32f
        canvas.drawText("   1 x 600.00 = 600.00", 60f, y, bodyPaint); y += 38f

        canvas.drawLine(60f, y, width - 60f, y, linePaint); y += 42f
        canvas.drawText("Taxable Value:               4,765.00", 60f, y, bodyPaint); y += 34f
        canvas.drawText("CGST 9%:                       428.85", 60f, y, bodyPaint); y += 34f
        canvas.drawText("SGST 9%:                       428.85", 60f, y, bodyPaint); y += 34f
        canvas.drawText("Round Off:                      +0.30", 60f, y, bodyPaint); y += 40f
        canvas.drawText("GRAND TOTAL:               Rs. 5,623", 60f, y, titlePaint)
        return bmp
    }

    private fun scaleBitmapForPayload(bitmap: Bitmap): Bitmap {
        val maxDim = 1400
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    internal fun parseScannedBillJson(rawJson: String): ScannedBillDraft {
        val json = JSONObject(rawJson)
        val itemsArr = json.optJSONArray("items")
        val parsedItems = mutableListOf<ScannedBillItem>()
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val obj = itemsArr.getJSONObject(i)
                val subsArr = obj.optJSONArray("subItems")
                val subs = mutableListOf<ScannedSubItem>()
                if (subsArr != null) {
                    for (j in 0 until subsArr.length()) {
                        val sObj = subsArr.getJSONObject(j)
                        subs += ScannedSubItem(
                            name = sObj.optString("name", "Part"),
                            qty = sObj.optDouble("qty", 1.0),
                            rate = sObj.optDouble("rate", 0.0)
                        )
                    }
                }
                val mappedRaw = obj.optString("mappedServiceItemId", "").trim()
                val noteRaw = obj.optString("lowConfidenceNote", "").trim()
                parsedItems += ScannedBillItem(
                    id = i + 1,
                    name = obj.optString("name", "Item ${i + 1}"),
                    qty = obj.optDouble("qty", 1.0),
                    unit = obj.optString("unit", "pcs"),
                    rate = obj.optDouble("rate", 0.0),
                    discount = obj.optDouble("discount", 0.0),
                    hsn = obj.optString("hsn", ""),
                    gstRate = obj.optDouble("gstRate", 18.0),
                    mappedServiceItemId = mappedRaw.takeIf { it.isNotEmpty() && it != "null" },
                    lowConfidenceNote = noteRaw.takeIf { it.isNotEmpty() && it != "null" },
                    subItems = subs
                )
            }
        }
        return ScannedBillDraft(
            merchant = json.optString("merchant", "Unknown merchant"),
            gstin = json.optString("gstin", ""),
            billNumber = json.optString("billNumber", ""),
            dateEpochMs = System.currentTimeMillis(),
            confidencePercent = json.optInt("confidencePercent", 92).coerceIn(1, 100),
            suggestedCategoryId = json.optString("suggestedCategoryId", "veh"),
            suggestedSubcategory = json.optString("suggestedSubcategory", "Service"),
            selectedVehicleId = "v1",
            isInterStateIgst = json.optBoolean("isInterStateIgst", false),
            keepItemLevel = true,
            items = parsedItems
        )
    }
}
