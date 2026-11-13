package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.SelavuRepository
import com.example.data.model.Bill
import com.example.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SelavuRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObserveExpense_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SelavuRepository(firestore)
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                Expense(
                    id = expId,
                    title = "Test Groceries",
                    amount = 1450.0,
                    dateEpochMs = System.currentTimeMillis(),
                    categoryId = "groc",
                    subcategory = "Groceries",
                    paymentMode = "UPI"
                )
            )
        }

        val expenses = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses(uid).first { list -> list.any { it.id == expId } }
        }
        assertTrue(expenses.any { it.id == expId && it.amount == 1450.0 })
    }

    @Test
    fun addVehicle_authenticatedOwner_seedsDefaultChecklist() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SelavuRepository(firestore)

        val vehicleId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addVehicle(
                name = "Honda Activa 6G",
                type = "bike",
                plate = "TN 01 XY 1234",
                odometer = 4200,
                avgMileage = 45.0
            )
        }

        val vehicles = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeVehicles(uid).first { list -> list.any { it.id == vehicleId } }
        }
        val added = vehicles.first { it.id == vehicleId }
        assertEquals(8, added.items.size)
        assertEquals(4200, added.odometer)
    }

    @Test
    fun markBillPaid_authenticatedOwner_createsExpenseAndNextDueBill() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SelavuRepository(firestore)

        val billId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBill(
                Bill(
                    name = "TNEB Rule Test",
                    amount = 980.0,
                    dueEpochMs = System.currentTimeMillis(),
                    repeat = "Monthly",
                    reminder = "3 days before",
                    categoryId = "util",
                    subcategory = "EB bill",
                    iconKey = "bolt"
                )
            )
        }

        val bill = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBills(uid).first { list -> list.any { it.id == billId } }
        }.first { it.id == billId }

        val nextDue = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markBillPaid(bill)
        }
        assertNotNull(nextDue)

        val billsAfter = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBills(uid).first { list ->
                list.any { it.id == billId && it.paid } && list.any { it.name == "TNEB Rule Test" && !it.paid }
            }
        }
        assertTrue(billsAfter.any { it.id == billId && it.paid })
    }

    @Test
    fun crossUserRead_isDeniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SelavuRepository(firestore)
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addExpense(
                Expense(
                    id = expId,
                    title = "Alice Private",
                    amount = 500.0,
                    dateEpochMs = System.currentTimeMillis(),
                    categoryId = "groc",
                    subcategory = "Groceries",
                    paymentMode = "Cash"
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("expenses").document(expId)
                    .get().await()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
            denied = true
        }
        assertTrue("Cross-user read should fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun observeExpenses_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SelavuRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeExpenses("alice_unauth_test").first()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
            denied = true
        }
        assertTrue("Unauthenticated observe should fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun ensureSampleDataSeeded_newUser_createsProfileAndCategoriesWithZeroDemoTransactions() = runBlocking {
        val freshEmail = "fresh_${UUID.randomUUID().toString().take(8)}@test.com"
        val uid = signInTestUser(freshEmail)
        val repository = SelavuRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(uid, "Fresh User")
        }

        val categories = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(uid).first { it.isNotEmpty() }
        }
        val expenses = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(uid).first() }
        val vehicles = withTimeout(FLOW_TIMEOUT_MS) { repository.observeVehicles(uid).first() }
        val bills = withTimeout(FLOW_TIMEOUT_MS) { repository.observeBills(uid).first() }
        val projects = withTimeout(FLOW_TIMEOUT_MS) { repository.observeProjects(uid).first() }

        assertEquals(9, categories.size)
        assertTrue(expenses.isEmpty())
        assertTrue(vehicles.isEmpty())
        assertTrue(bills.isEmpty())
        assertTrue(projects.isEmpty())
    }

    @Test
    fun deleteAllUserData_authenticatedOwner_wipesAllSubcollections() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SelavuRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(uid, "Alice")
            repository.addExpense(
                Expense(
                    title = "Coffee",
                    amount = 120.0,
                    dateEpochMs = System.currentTimeMillis(),
                    categoryId = "food",
                    subcategory = "Cafes",
                    paymentMode = "UPI"
                )
            )
            repository.addVehicle("Activa", "bike", "TN 01 AB 1234", 1200, 45.0)
            repository.addBill(
                Bill(
                    name = "Internet",
                    amount = 999.0,
                    dueEpochMs = System.currentTimeMillis(),
                    repeat = "Monthly",
                    reminder = "3 days before",
                    categoryId = "util",
                    subcategory = "Internet",
                    iconKey = "wifi"
                )
            )
            repository.addProject("Kitchen", 250000.0, "Renovation")
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteAllUserData(uid, deleteProfileDoc = false)
        }

        assertTrue(withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(uid).first() }.isEmpty())
        assertTrue(withTimeout(FLOW_TIMEOUT_MS) { repository.observeVehicles(uid).first() }.isEmpty())
        assertTrue(withTimeout(FLOW_TIMEOUT_MS) { repository.observeBills(uid).first() }.isEmpty())
        assertTrue(withTimeout(FLOW_TIMEOUT_MS) { repository.observeProjects(uid).first() }.isEmpty())
        assertTrue(withTimeout(FLOW_TIMEOUT_MS) { repository.observeCategories(uid).first() }.isEmpty())
    }

    @Test
    fun crossUserDelete_isDeniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SelavuRepository(firestore)
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addExpense(
                Expense(
                    id = expId,
                    title = "Alice Private Expense",
                    amount = 750.0,
                    dateEpochMs = System.currentTimeMillis(),
                    categoryId = "groc",
                    subcategory = "Groceries",
                    paymentMode = "UPI"
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("expenses").document(expId)
                    .delete().await()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
            denied = true
        }
        assertTrue("Cross-user delete should fail with PERMISSION_DENIED", denied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
