package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "BillScannerService"
private const val MAX_IMAGE_DIM = 1600

private val VALID_CATEGORY_IDS = setOf(
    "veh", "home", "groc", "util", "subs", "hlth", "biz", "food", "fin"
)

class BillScannerService {

    private val billSchema = Schema.obj(
        properties = mapOf(
            "merchant" to Schema.string(description = "Shop, petrol pump, garage, restaurant, or utility provider name"),
            "gstin" to Schema.string(description = "15-char Indian GSTIN if printed on bill, else empty string"),
            "billNumber" to Schema.string(description = "Invoice, receipt, or bill number if printed, else empty string"),
            "billDate" to Schema.string(description = "Receipt date in YYYY-MM-DD format if clearly visible, else empty string"),
            "confidencePercent" to Schema.integer(description = "Overall extraction confidence 0 to 100"),
            "suggestedCategoryId" to Schema.enumeration(
                values = listOf("veh", "home", "groc", "util", "subs", "hlth", "biz", "food", "fin"),
                description = "Best matching category ID"
            ),
            "suggestedSubcategory" to Schema.string(
                description = "Best matching subcategory such as Service, Fuel, Groceries, EB bill, Pharmacy, Restaurants, Materials, Rent"
            ),
            "isInterStateIgst" to Schema.boolean(description = "True if IGST applies instead of CGST+SGST"),
            "grandTotal" to Schema.double(description = "Final printed grand total / net amount in INR if visible, else 0"),
            "items" to Schema.array(
                description = "Extracted line items (always include at least 1 item, even for fuel slips or total-only POS receipts)",
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Item, fuel type (e.g. Petrol), or service description"),
                        "qty" to Schema.double(description = "Quantity (e.g. litres for fuel, count for parts)"),
                        "unit" to Schema.string(description = "Unit e.g. L, pcs, hrs, kg, bill"),
                        "rate" to Schema.double(description = "Per-unit rate in INR (pre-GST when GST is added below, or final inclusive unit price when gstRate is 0)"),
                        "discount" to Schema.double(description = "Line discount in INR, 0 if none"),
                        "hsn" to Schema.string(description = "HSN or SAC code if present, else empty string"),
                        "gstRate" to Schema.double(description = "Exclusive GST percentage added on top (0, 5, 12, 18, 28). Set 0 for fuel or tax-inclusive/non-GST receipts."),
                        "mappedServiceItemId" to Schema.string(
                            description = "Matching vehicle service key if vehicle service: oil, oilf, chain, lube, align, gen, air, brake, ins, tyre, ac, batt, or empty string"
                        ),
                        "lowConfidenceNote" to Schema.string(
                            description = "Warning message if a handwritten rate or blurry figure needs review, else empty string"
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
        ),
        optionalProperties = listOf("billDate", "grandTotal")
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

    suspend fun extractBillFromBitmap(bitmap: Bitmap): ScannedBillDraft = withContext(Dispatchers.IO) {
        val scaled = scaleBitmapForPayload(bitmap)
        val promptText = """
            You are an Indian receipt, fuel slip, and GST bill extraction engine for a personal finance app.
            The receipt photo may be rotated, printed on thermal paper, handwritten, or a PDF/POS charge slip.
            Extract merchant name, GSTIN (if printed), bill/invoice number, billDate (YYYY-MM-DD if visible), grandTotal, and all line items accurately.

            Category & GST rules:
            1. Fuel / Petrol / Diesel / CNG slips (e.g. IndianOil, HPCL, BPCL, Shell, Nayara, Paytm/POS fuel slips):
               - Set suggestedCategoryId="veh", suggestedSubcategory="Fuel".
               - Fuel is NOT GST-billed: ALWAYS set gstRate=0.
               - Use unit="L", qty=litres (e.g. 31.28), rate=unit price per litre (e.g. 103.32) so qty*rate equals Total Sale / Net Amount.
            2. Vehicle servicing / repairs / spare parts:
               - Set suggestedCategoryId="veh", suggestedSubcategory="Service".
               - Map each line to mappedServiceItemId from: oil, oilf, chain, lube, align, gen, air, brake, ins, tyre, ac, batt (or "" if none matches).
               - If a line is a bundled kit (like "Chain & sprocket kit"), split itemized parts into subItems.
            3. Tax & totals:
               - If CGST/SGST/IGST is added separately below a Taxable Value subtotal, set rate=pre-tax unit price and gstRate=total GST % (e.g. 5, 12, 18, 28).
               - If prices are already tax-inclusive (MRP, grocery, pharmacy, fuel, POS card slip) or no tax breakdown appears, set gstRate=0 so the sum equals grandTotal.
            4. POS / total-only / handwritten bills:
               - Always return at least 1 item in items (e.g. qty=1, unit="bill", rate=grandTotal if no line items exist).
               - Flag any handwritten, smudged, faded, or uncertain number in lowConfidenceNote.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(scaled)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI returned an empty response.")
            parseScannedBillJson(rawJson)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Bill extraction timed out", e)
            throw IllegalStateException("Bill scan timed out after 60 seconds. Please check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI bill extraction failed", e)
            throw IllegalStateException(formatExtractionError(e), e)
        }
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        val isPdf = mime.contains("pdf", ignoreCase = true) ||
            uri.toString().endsWith(".pdf", ignoreCase = true)

        if (isPdf) {
            val pfd: ParcelFileDescriptor = resolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Unable to open PDF file.")
            pfd.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount == 0) throw IllegalStateException("PDF has no pages.")
                    renderer.openPage(0).use { page ->
                        val pageW = page.width.coerceAtLeast(1)
                        val pageH = page.height.coerceAtLeast(1)
                        val scale = minOf(
                            MAX_IMAGE_DIM.toFloat() / pageW.toFloat(),
                            2200f / pageH.toFloat(),
                            2f
                        ).coerceAtLeast(0.5f)
                        val targetW = (pageW * scale).roundToInt().coerceAtLeast(1)
                        val targetH = (pageH * scale).roundToInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        return@withContext bmp
                    }
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: throw IllegalStateException("Unable to read selected image.")

            val orientation = try {
                resolver.openInputStream(uri)?.use { exifStream ->
                    ExifInterface(exifStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_DIM)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = try {
                resolver.openInputStream(uri)?.use { decodeStream ->
                    BitmapFactory.decodeStream(decodeStream, null, decodeOpts)
                }
            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "OOM decoding image URI", oom)
                throw IllegalStateException("Image is too large to process in memory. Please crop or try another photo.")
            } ?: throw IllegalStateException("Unsupported or corrupted image file.")

            applyExifOrientation(decoded, orientation)
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
        val w = bitmap.width
        val h = bitmap.height
        if (w <= MAX_IMAGE_DIM && h <= MAX_IMAGE_DIM) return bitmap
        val ratio = MAX_IMAGE_DIM.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).roundToInt().coerceAtLeast(1),
            (h * ratio).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var inSampleSize = 1
        val longest = maxOf(width, height)
        while ((longest / inSampleSize) > maxDim * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    internal fun parseScannedBillJson(rawJson: String, nowMs: Long = System.currentTimeMillis()): ScannedBillDraft {
        val cleanJson = extractJsonObjectText(rawJson)
        val json = JSONObject(cleanJson)
        val itemsArr = json.optJSONArray("items")
        val parsedItems = mutableListOf<ScannedBillItem>()
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val obj = itemsArr.optJSONObject(i) ?: continue
                val subsArr = obj.optJSONArray("subItems")
                val subs = mutableListOf<ScannedSubItem>()
                if (subsArr != null) {
                    for (j in 0 until subsArr.length()) {
                        val sObj = subsArr.optJSONObject(j) ?: continue
                        val subQty = sanitizePositive(sObj.optDouble("qty", 1.0), fallback = 1.0)
                        val subRate = sanitizeNonNegative(sObj.optDouble("rate", 0.0))
                        subs += ScannedSubItem(
                            name = sObj.optString("name", "Part").trim().ifBlank { "Part" },
                            qty = subQty,
                            rate = subRate
                        )
                    }
                }
                val mappedRaw = obj.optString("mappedServiceItemId", "").trim()
                val noteRaw = obj.optString("lowConfidenceNote", "").trim()
                val rawQty = sanitizePositive(obj.optDouble("qty", 1.0), fallback = 1.0)
                val rawRate = sanitizeNonNegative(obj.optDouble("rate", 0.0))
                val rawDiscount = sanitizeNonNegative(obj.optDouble("discount", 0.0))
                val rawGst = sanitizeNonNegative(obj.optDouble("gstRate", 0.0)).coerceAtMost(28.0)

                parsedItems += ScannedBillItem(
                    id = parsedItems.size + 1,
                    name = obj.optString("name", "Item ${parsedItems.size + 1}").trim().ifBlank { "Item ${parsedItems.size + 1}" },
                    qty = rawQty,
                    unit = obj.optString("unit", "pcs").trim().ifBlank { "pcs" },
                    rate = rawRate,
                    discount = rawDiscount,
                    hsn = obj.optString("hsn", "").trim().takeUnless { it == "null" }.orEmpty(),
                    gstRate = rawGst,
                    mappedServiceItemId = mappedRaw.takeIf { it.isNotEmpty() && it != "null" },
                    lowConfidenceNote = noteRaw.takeIf { it.isNotEmpty() && it != "null" },
                    subItems = subs
                )
            }
        }

        val printedTotal = sanitizeNonNegative(json.optDouble("grandTotal", 0.0))
        val merchant = json.optString("merchant", "Unknown merchant")
            .trim()
            .takeUnless { it == "null" || it.isBlank() }
            ?: "Unknown merchant"

        if (parsedItems.isEmpty() && printedTotal > 0.0) {
            parsedItems += ScannedBillItem(
                id = 1,
                name = merchant,
                qty = 1.0,
                unit = "bill",
                rate = printedTotal,
                gstRate = 0.0
            )
        }

        val rawCategory = json.optString("suggestedCategoryId", "groc").trim().lowercase()
        val categoryId = if (rawCategory in VALID_CATEGORY_IDS) rawCategory else "groc"
        val rawSubcategory = json.optString("suggestedSubcategory", "")
            .trim()
            .takeUnless { it == "null" }
            .orEmpty()
            .ifBlank {
                when (categoryId) {
                    "veh" -> "Service"
                    "food" -> "Restaurants"
                    "util" -> "EB bill"
                    "hlth" -> "Pharmacy"
                    else -> "Groceries"
                }
            }

        val parsedDateMs = parseBillDateEpochMs(
            json.optString("billDate", "").trim().takeUnless { it == "null" }.orEmpty(),
            nowMs
        )

        return ScannedBillDraft(
            merchant = merchant,
            gstin = json.optString("gstin", "").trim().takeUnless { it == "null" }.orEmpty(),
            billNumber = json.optString("billNumber", "").trim().takeUnless { it == "null" }.orEmpty(),
            dateEpochMs = parsedDateMs,
            confidencePercent = json.optInt("confidencePercent", 90).coerceIn(1, 100),
            suggestedCategoryId = categoryId,
            suggestedSubcategory = rawSubcategory,
            selectedVehicleId = if (categoryId == "veh") "v1" else null,
            isInterStateIgst = json.optBoolean("isInterStateIgst", false),
            keepItemLevel = true,
            items = parsedItems
        )
    }

    private fun extractJsonObjectText(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    private fun parseBillDateEpochMs(rawDate: String, fallbackNowMs: Long): Long {
        if (rawDate.isBlank()) return fallbackNowMs
        val formats = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy")
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(rawDate) ?: continue
                val cal = Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val year = cal.get(Calendar.YEAR)
                if (year in 2015..2035) {
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return fallbackNowMs
    }

    private fun sanitizePositive(value: Double, fallback: Double): Double =
        if (value.isNaN() || value.isInfinite() || value <= 0.0) fallback else value

    private fun sanitizeNonNegative(value: Double): Double =
        if (value.isNaN() || value.isInfinite() || value < 0.0) 0.0 else value

    private fun formatExtractionError(error: Exception): String {
        val chainMessages = generateSequence<Throwable>(error) { it.cause }
            .mapNotNull { it.message?.takeIf(String::isNotBlank) }
            .toList()
        val combined = chainMessages.joinToString(" · ")
        val lower = combined.lowercase()
        return when {
            "app check" in lower || "appcheck" in lower || "403" in lower || "attestation" in lower ->
                "AI verification failed (App Check / HTTP 403). Please update or reinstall the latest build from AI Studio and try again."
            error is IOException || "unable to resolve host" in lower || "failed to connect" in lower || "network" in lower ->
                "Network error while contacting Gemini AI. Check your internet connection and retry."
            "quota" in lower || "429" in lower || "resource exhausted" in lower ->
                "AI scan rate limit reached. Please wait a moment and try again."
            combined.isNotBlank() -> combined.take(220)
            else -> "Failed to scan bill with AI (${error::class.java.simpleName})."
        }
    }
}
