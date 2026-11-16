package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelavuModelsEnhancementsTest {

    @Test
    fun expenseToCreateMap_includesVehicleAndOptionalFuelMetadataWhenProvided() {
        val expense = Expense(
            title = "Activa fuel",
            amount = 525.0,
            dateEpochMs = 1_700_000_000_000L,
            categoryId = "veh",
            subcategory = "Fuel",
            paymentMode = "UPI",
            vehicleId = "veh_1",
            odometerReading = 18420,
            fuelPricePerLitre = 105.0,
            fuelLitres = 5.0
        )

        val map = expense.toCreateMap("uid_1")
        assertEquals("veh_1", map["vehicleId"])
        assertEquals(18420, map["odometerReading"])
        assertEquals(105.0, map["fuelPricePerLitre"] as Double, 0.001)
        assertEquals(5.0, map["fuelLitres"] as Double, 0.001)
    }

    @Test
    fun expenseToCreateMap_omitsInvalidOrEmptyFuelMetadata() {
        val expense = Expense(
            title = "Fuel without details",
            amount = 300.0,
            dateEpochMs = 1_700_000_000_000L,
            categoryId = "veh",
            subcategory = "Fuel",
            vehicleId = "veh_1",
            odometerReading = -10,
            fuelPricePerLitre = 0.0,
            fuelLitres = null
        )

        val map = expense.toCreateMap("uid_1")
        assertFalse(map.containsKey("odometerReading"))
        assertFalse(map.containsKey("fuelPricePerLitre"))
        assertFalse(map.containsKey("fuelLitres"))
    }

    @Test
    fun projectEffectiveCatalog_fallsBackToDefaultsWithConfiguredUnits_andPreservesCustomCatalog() {
        val emptyCatalogProject = Project(name = "Home Build", budget = 500_000.0)
        val defaults = emptyCatalogProject.effectiveProductCatalog()
        assertTrue(defaults.any { it.name == "Bricks" && it.unit == "count" })
        assertTrue(defaults.any { it.name == "Steel" && it.unit == "per kg" })
        assertTrue(defaults.any { it.name == "Cement" && it.unit == "per bag" })

        val custom = listOf(ProjectProduct(id = "tmt", name = "TMT Rods", unit = "per kg"))
        val customProject = Project(name = "Renovation", budget = 100_000.0, productCatalog = custom)
        assertEquals(custom, customProject.effectiveProductCatalog())
    }

    @Test
    fun projectPaymentToMap_serializesGranularLineItemsWithUnits() {
        val item = ProjectPaymentItem(
            productId = "bricks",
            name = "Bricks",
            unit = "count",
            qty = 2000.0,
            unitPrice = 9.0,
            totalPrice = 18000.0
        )
        val payment = ProjectPayment(
            dateEpochMs = 1_700_000_000_000L,
            title = "Foundation bricks",
            amount = 18000.0,
            vendor = "Sri Murugan Kiln",
            phaseName = "Foundation",
            items = listOf(item)
        )

        val map = payment.toMap()
        @Suppress("UNCHECKED_CAST")
        val encodedItems = map["items"] as List<Map<String, Any>>
        assertEquals(1, encodedItems.size)
        assertEquals("Bricks", encodedItems.first()["name"])
        assertEquals("count", encodedItems.first()["unit"])
        assertEquals(2000.0, encodedItems.first()["qty"] as Double, 0.001)
        assertEquals(9.0, encodedItems.first()["unitPrice"] as Double, 0.001)
        assertEquals(18000.0, encodedItems.first()["totalPrice"] as Double, 0.001)
    }
}
