package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class BillScannerServiceTest {

    private val scanner = BillScannerService()

    @Test
    fun parseScannedBillJson_indianOilFuelReceipt_parsesLitresZeroGstAndDate() {
        val rawJson = """
            {
              "merchant": "IndianOil - Vmcs and skrs associates",
              "gstin": "",
              "billNumber": "000445",
              "billDate": "2026-11-14",
              "confidencePercent": 96,
              "suggestedCategoryId": "veh",
              "suggestedSubcategory": "Fuel",
              "isInterStateIgst": false,
              "grandTotal": 3231.85,
              "items": [
                {
                  "name": "Petrol",
                  "qty": 31.28,
                  "unit": "L",
                  "rate": 103.32,
                  "discount": 0.0,
                  "hsn": "",
                  "gstRate": 0.0,
                  "mappedServiceItemId": "",
                  "lowConfidenceNote": ""
                }
              ]
            }
        """.trimIndent()

        val draft = scanner.parseScannedBillJson(rawJson, nowMs = 1_700_000_000_000L)

        assertEquals("IndianOil - Vmcs and skrs associates", draft.merchant)
        assertEquals("000445", draft.billNumber)
        assertEquals("veh", draft.suggestedCategoryId)
        assertEquals("Fuel", draft.suggestedSubcategory)
        assertEquals("v1", draft.selectedVehicleId)
        assertEquals(96, draft.confidencePercent)
        assertEquals(1, draft.items.size)

        val item = draft.items.first()
        assertEquals("Petrol", item.name)
        assertEquals(31.28, item.qty, 0.001)
        assertEquals("L", item.unit)
        assertEquals(103.32, item.rate, 0.001)
        assertEquals(0.0, item.gstRate, 0.001)
        assertNull(item.mappedServiceItemId)

        val cal = Calendar.getInstance().apply { timeInMillis = draft.dateEpochMs }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(14, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parseScannedBillJson_garageInvoiceWithSubItemsAndHandwrittenWarning_parsesCorrectly() {
        val rawJson = """
            {
              "merchant": "Sri Vel Two-Wheeler Care",
              "gstin": "33ABCDE1234F1Z5",
              "billNumber": "INV-20931",
              "billDate": "12-11-2026",
              "confidencePercent": 92,
              "suggestedCategoryId": "veh",
              "suggestedSubcategory": "Service",
              "isInterStateIgst": false,
              "grandTotal": 5623.0,
              "items": [
                {
                  "name": "Chain & sprocket kit",
                  "qty": 1,
                  "unit": "kit",
                  "rate": 2350.0,
                  "discount": 0,
                  "hsn": "8714",
                  "gstRate": 18,
                  "mappedServiceItemId": "chain",
                  "lowConfidenceNote": "",
                  "subItems": [
                    { "name": "Drive chain 520", "qty": 1, "rate": 1450.0 },
                    { "name": "Front sprocket", "qty": 1, "rate": 420.0 }
                  ]
                },
                {
                  "name": "Wheel alignment",
                  "qty": 1,
                  "unit": "hrs",
                  "rate": 450.0,
                  "discount": 0,
                  "hsn": "9987",
                  "gstRate": 18,
                  "mappedServiceItemId": "align",
                  "lowConfidenceNote": "Handwritten rate"
                }
              ]
            }
        """.trimIndent()

        val draft = scanner.parseScannedBillJson(rawJson, nowMs = 1_700_000_000_000L)

        assertEquals("Sri Vel Two-Wheeler Care", draft.merchant)
        assertEquals(2, draft.items.size)
        assertEquals("chain", draft.items[0].mappedServiceItemId)
        assertEquals(2, draft.items[0].subItems.size)
        assertNotNull(draft.items[1].lowConfidenceNote)
        assertEquals("Handwritten rate", draft.items[1].lowConfidenceNote)
    }

    @Test
    fun parseScannedBillJson_emptyItemsWithGrandTotal_createsSingleFallbackItem() {
        val rawJson = """
            ```json
            {
              "merchant": "Local Bakery",
              "gstin": "",
              "billNumber": "",
              "confidencePercent": 80,
              "suggestedCategoryId": "invalid_cat",
              "suggestedSubcategory": "",
              "isInterStateIgst": false,
              "grandTotal": 485.0,
              "items": []
            }
            ```
        """.trimIndent()

        val draft = scanner.parseScannedBillJson(rawJson, nowMs = 1_700_000_000_000L)

        assertEquals("Local Bakery", draft.merchant)
        assertEquals("groc", draft.suggestedCategoryId)
        assertEquals(1, draft.items.size)
        assertEquals(485.0, draft.items.first().rate, 0.001)
        assertEquals(0.0, draft.items.first().gstRate, 0.001)
    }
}
