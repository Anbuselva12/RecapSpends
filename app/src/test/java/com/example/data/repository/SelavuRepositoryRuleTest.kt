package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.SelavuRepository
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun workspaceLifecycle_privacyKickAndLeave_enforcesRulesAndRemovesSharedData() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "ws_alice_$uniqueSuffix@test.com"
        val bobEmail = "ws_bob_$uniqueSuffix@test.com"
        val charlieEmail = "ws_charlie_$uniqueSuffix@test.com"

        val repository = SelavuRepository(firestore)

        // 1. Alice creates profile, categories (1 hidden), expenses (1 public, 1 private, 1 hidden category), and workspace
        val aliceUid = signInTestUser(aliceEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(aliceUid, "Alice")
        }
        val secretCatId = "cat_secret_$uniqueSuffix"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCategory(
                Category(
                    id = "",
                    name = "Personal Care",
                    iconKey = "heart",
                    colorIndex = 5,
                    subcategories = listOf("Clinic"),
                    sortOrder = 20,
                    hiddenFromWorkspace = true
                )
            )
        }
        // Fetch categories to get the actual ID of Personal Care
        val aliceCats = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(aliceUid).first { cats -> cats.any { it.hiddenFromWorkspace } }
        }
        val hiddenCatId = aliceCats.first { it.hiddenFromWorkspace }.id

        val publicExpId = "exp_pub_$uniqueSuffix"
        val privateExpId = "exp_priv_$uniqueSuffix"
        val hiddenCatExpId = "exp_hcat_$uniqueSuffix"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                Expense(
                    id = publicExpId,
                    title = "Family Milk",
                    amount = 320.0,
                    categoryId = "groc",
                    subcategory = "Dairy",
                    paymentMode = "UPI",
                    isPrivate = false
                )
            )
            repository.addExpense(
                Expense(
                    id = privateExpId,
                    title = "Surprise Watch",
                    amount = 4500.0,
                    categoryId = "groc",
                    subcategory = "Gift",
                    paymentMode = "Card",
                    isPrivate = true
                )
            )
            repository.addExpense(
                Expense(
                    id = hiddenCatExpId,
                    title = "Doctor Visit",
                    amount = 900.0,
                    categoryId = hiddenCatId,
                    subcategory = "Clinic",
                    paymentMode = "UPI",
                    isPrivate = false
                )
            )
        }

        val ws = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWorkspace("Home Budget", "Alice")
        }
        assertEquals(aliceUid, ws.ownerUid)
        assertTrue(ws.inviteCode.length >= 6)

        // 2. Bob signs up, joins workspace with invite code
        val bobUid = signInTestUser(bobEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(bobUid, "Bob")
        }
        val joinedWsId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinWorkspaceByCode(ws.inviteCode, "Bob")
        }
        assertEquals(ws.id, joinedWsId)

        val bobExpId = "exp_bob_$uniqueSuffix"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                Expense(
                    id = bobExpId,
                    title = "Bob Fuel",
                    amount = 1100.0,
                    categoryId = "veh",
                    subcategory = "Fuel",
                    paymentMode = "UPI",
                    isPrivate = false
                )
            )
        }
        val bobCats = withTimeout(FLOW_TIMEOUT_MS) { repository.observeCategories(bobUid).first { it.isNotEmpty() } }
        val bobExps = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(bobUid).first { it.any { e -> e.id == bobExpId } } }
        val wsForBob = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && bobUid in it.memberIds }
        }!!
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyWorkspaceShares(bobUid, "Bob", wsForBob, bobCats, bobExps)
        }

        // 3. Alice syncs her shares with updated memberIds ([aliceUid, bobUid])
        signInTestUser(aliceEmail)
        val aliceExps = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(aliceUid).first { it.size >= 3 } }
        val wsForAlice = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && bobUid in it.memberIds }
        }!!
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyWorkspaceShares(aliceUid, "Alice", wsForAlice, aliceCats, aliceExps)
        }

        val aliceShared = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedExpenses(ws.id, aliceUid).first { list ->
                list.any { it.sourceExpenseId == publicExpId } && list.any { it.sourceExpenseId == bobExpId }
            }
        }
        // Only Alice's non-private, non-hidden-category expense + Bob's expense appear
        assertTrue(aliceShared.any { it.sourceExpenseId == publicExpId })
        assertFalse(aliceShared.any { it.sourceExpenseId == privateExpId })
        assertFalse(aliceShared.any { it.sourceExpenseId == hiddenCatExpId })
        assertTrue(aliceShared.any { it.sourceExpenseId == bobExpId && it.ownerUid == bobUid })

        // 4. Bob checks shared view and "Others only" filter
        signInTestUser(bobEmail)
        val bobSharedView = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedExpenses(ws.id, bobUid).first { list ->
                list.any { it.sourceExpenseId == publicExpId }
            }
        }
        val onlyOthers = bobSharedView.filter { it.ownerUid != bobUid }
        assertEquals(1, onlyOthers.size)
        assertEquals(publicExpId, onlyOthers.first().sourceExpenseId)

        // 5. Alice kicks Bob: Bob's shared expense is removed completely; Bob loses workspace access; Bob's personal expense remains intact
        signInTestUser(aliceEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.removeMemberFromWorkspace(wsForAlice, bobUid)
        }

        val afterKickWs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && bobUid !in it.memberIds }
        }!!
        assertFalse(bobUid in afterKickWs.memberIds)
        val sharedAfterKick = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedExpenses(ws.id, aliceUid).first { list -> list.none { it.ownerUid == bobUid } }
        }
        assertTrue(sharedAfterKick.none { it.ownerUid == bobUid })

        signInTestUser(bobEmail)
        // Raw Firestore get fails with PERMISSION_DENIED for kicked Bob
        var bobDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("workspaces").document(ws.id).get().await()
            }
        } catch (e: Exception) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.unwrapFirestoreException()?.code)
            bobDenied = true
        }
        assertTrue("Kicked member must receive PERMISSION_DENIED on direct workspace get", bobDenied)
        // observeWorkspace emits null cleanly instead of crashing
        assertNull(withTimeout(FLOW_TIMEOUT_MS) { repository.observeWorkspace(ws.id).first() })
        // Bob's personal expense is untouched
        val bobPersonalAfterKick = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(bobUid).first() }
        assertTrue(bobPersonalAfterKick.any { it.id == bobExpId })

        // 6. Charlie joins and voluntarily leaves -> Charlie's shared expense deleted, personal kept
        val charlieUid = signInTestUser(charlieEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(charlieUid, "Charlie")
            repository.joinWorkspaceByCode(ws.inviteCode, "Charlie")
        }
        val charlieExpId = "exp_charlie_$uniqueSuffix"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                Expense(
                    id = charlieExpId,
                    title = "Charlie Snacks",
                    amount = 250.0,
                    categoryId = "food",
                    subcategory = "Snacks",
                    paymentMode = "Cash",
                    isPrivate = false
                )
            )
        }
        val charlieCats = withTimeout(FLOW_TIMEOUT_MS) { repository.observeCategories(charlieUid).first { it.isNotEmpty() } }
        val charlieExps = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(charlieUid).first { it.any { e -> e.id == charlieExpId } } }
        val wsForCharlie = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && charlieUid in it.memberIds }
        }!!
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyWorkspaceShares(charlieUid, "Charlie", wsForCharlie, charlieCats, charlieExps)
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.leaveWorkspace(ws.id)
        }
        val charlieProfileAfterLeave = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(charlieUid).first { it != null && it.workspaceId == null }
        }!!
        assertNull(charlieProfileAfterLeave.workspaceId)
        val charliePersonalAfterLeave = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(charlieUid).first() }
        assertTrue(charliePersonalAfterLeave.any { it.id == charlieExpId })

        // Cleanup workspace as Alice
        signInTestUser(aliceEmail)
        val finalWs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && charlieUid !in it.memberIds }
        }!!
        assertFalse(charlieUid in finalWs.memberIds)
        val finalShared = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedExpenses(ws.id, aliceUid).first { list -> list.none { it.ownerUid == charlieUid } }
        }
        assertTrue(finalShared.none { it.ownerUid == charlieUid })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteWorkspace(finalWs)
        }
    }

    @Test
    fun updateExpense_andUpdateDeleteBill_authenticatedOwner_succeed() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SelavuRepository(firestore)

        val expId = "exp_edit_${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                Expense(
                    id = expId,
                    title = "Initial Shop",
                    amount = 300.0,
                    categoryId = "groc",
                    subcategory = "Groceries",
                    paymentMode = "Cash",
                    vehicleId = "v_temp",
                    projectId = "p_temp"
                )
            )
        }

        val created = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses(uid).first { list -> list.any { it.id == expId } }
        }.first { it.id == expId }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateExpense(
                created.copy(
                    title = "Updated Fuel Stop",
                    amount = 950.0,
                    categoryId = "veh",
                    subcategory = "Fuel",
                    paymentMode = "UPI",
                    vehicleId = "v_new",
                    projectId = null,
                    odometerReading = 5400,
                    fuelPricePerLitre = 102.0,
                    fuelLitres = 9.31
                )
            )
        }

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses(uid).first { list ->
                list.any { it.id == expId && it.title == "Updated Fuel Stop" && it.amount == 950.0 }
            }
        }.first { it.id == expId }
        assertEquals("veh", updated.categoryId)
        assertEquals("v_new", updated.vehicleId)
        assertNull(updated.projectId)
        assertEquals(5400, updated.odometerReading)

        val billId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBill(
                Bill(
                    name = "Original Fibre",
                    amount = 799.0,
                    dueEpochMs = System.currentTimeMillis() + 86_400_000L,
                    repeat = "Monthly",
                    reminder = "3 days before",
                    categoryId = "util",
                    subcategory = "Internet",
                    iconKey = "wifi",
                    hiddenFromWorkspace = false
                )
            )
        }
        val createdBill = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBills(uid).first { list -> list.any { it.id == billId } }
        }.first { it.id == billId }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateBill(createdBill.copy(name = "ACT Fibernet", amount = 1099.0, hiddenFromWorkspace = true))
        }
        val updatedBill = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBills(uid).first { list ->
                list.any { it.id == billId && it.name == "ACT Fibernet" && it.hiddenFromWorkspace }
            }
        }.first { it.id == billId }
        assertEquals(1099.0, updatedBill.amount, 0.01)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteBill(billId)
        }
        val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBills(uid).first { list -> list.none { it.id == billId } }
        }
        assertTrue(afterDelete.none { it.id == billId })
    }

    @Test
    fun workspaceSharedVehiclesProjectsBillsCategories_andDepartedContributorRetention() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "share_alice_$uniqueSuffix@test.com"
        val bobEmail = "share_bob_$uniqueSuffix@test.com"
        val carolEmail = "share_carol_$uniqueSuffix@test.com"
        val repository = SelavuRepository(firestore)

        // Alice sets up profile, visible & hidden vehicles/projects/bills, custom category, and workspace
        val aliceUid = signInTestUser(aliceEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(aliceUid, "Alice")
        }
        val aliceVehId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addVehicle("Shared Innova", "car", "TN 09 AB 1111", 10000, 14.0, hiddenFromWorkspace = false)
        }
        val aliceHiddenVehId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addVehicle("Secret Scooter", "bike", "TN 09 XY 9999", 500, 50.0, hiddenFromWorkspace = true)
        }
        val aliceProjId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addProject("Home Solar", 200000.0, "Renovation", hiddenFromWorkspace = false)
        }
        val aliceBillId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBill(
                Bill(
                    name = "Shared EB Bill",
                    amount = 1850.0,
                    dueEpochMs = System.currentTimeMillis() + 172_800_000L,
                    repeat = "Monthly",
                    reminder = "3 days before",
                    categoryId = "util",
                    subcategory = "EB bill",
                    iconKey = "bolt",
                    hiddenFromWorkspace = false
                )
            )
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCategory(
                Category(
                    id = "",
                    name = "Pets",
                    iconKey = "heart",
                    colorIndex = 4,
                    subcategories = listOf("Vet", "Food"),
                    sortOrder = 30,
                    hiddenFromWorkspace = false
                )
            )
        }
        val ws = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWorkspace("Shared Home", "Alice")
        }

        // Bob & Carol join
        val bobUid = signInTestUser(bobEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(bobUid, "Bob")
            repository.joinWorkspaceByCode(ws.inviteCode, "Bob")
        }
        val carolUid = signInTestUser(carolEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSampleDataSeeded(carolUid, "Carol")
            repository.joinWorkspaceByCode(ws.inviteCode, "Carol")
        }

        // Alice syncs shares now that Bob & Carol are in workspace
        signInTestUser(aliceEmail)
        val aliceCats = withTimeout(FLOW_TIMEOUT_MS) { repository.observeCategories(aliceUid).first { it.any { c -> c.name == "Pets" } } }
        val aliceExps = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(aliceUid).first() }
        val aliceVehs = withTimeout(FLOW_TIMEOUT_MS) { repository.observeVehicles(aliceUid).first { it.size >= 2 } }
        val aliceBills = withTimeout(FLOW_TIMEOUT_MS) { repository.observeBills(aliceUid).first { it.any { b -> b.id == aliceBillId } } }
        val aliceProjs = withTimeout(FLOW_TIMEOUT_MS) { repository.observeProjects(aliceUid).first { it.any { p -> p.id == aliceProjId } } }
        val wsUpdated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && bobUid in it.memberIds && carolUid in it.memberIds }
        }!!
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyWorkspaceShares(
                uid = aliceUid,
                ownerName = "Alice",
                workspace = wsUpdated,
                categories = aliceCats,
                expenses = aliceExps,
                vehicles = aliceVehs,
                bills = aliceBills,
                projects = aliceProjs
            )
        }

        // Bob sees visible shared vehicle, project, bill, and Pets category, but NOT hidden vehicle
        signInTestUser(bobEmail)
        val sharedVehs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedVehicles(ws.id, bobUid).first { list -> list.any { it.sourceVehicleId == aliceVehId } }
        }
        assertTrue(sharedVehs.any { it.sourceVehicleId == aliceVehId })
        assertFalse(sharedVehs.any { it.sourceVehicleId == aliceHiddenVehId })

        val sharedProjs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedProjects(ws.id, bobUid).first { list -> list.any { it.sourceProjectId == aliceProjId } }
        }
        val bobSharedProj = sharedProjs.first { it.sourceProjectId == aliceProjId }
        val bobSharedVeh = sharedVehs.first { it.sourceVehicleId == aliceVehId }

        val sharedBills = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedBills(ws.id, bobUid).first { list -> list.any { it.sourceBillId == aliceBillId } }
        }
        assertTrue(sharedBills.any { it.name == "Shared EB Bill" })

        val sharedCats = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedCategories(ws.id, bobUid).first { list -> list.any { it.name == "Pets" } }
        }
        assertTrue(sharedCats.any { it.name == "Pets" && "Vet" in it.subcategories })

        // Bob contributes to Alice's shared vehicle and shared project
        val bobFuelExpId = "exp_bob_fuel_$uniqueSuffix"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                Expense(
                    id = bobFuelExpId,
                    title = "Innova Diesel",
                    amount = 2200.0,
                    categoryId = "veh",
                    subcategory = "Fuel",
                    paymentMode = "UPI",
                    vehicleId = bobSharedVeh.id,
                    isPrivate = false
                )
            )
            repository.recordVehicleContributor(ws.id, bobSharedVeh.id, aliceUid)
            repository.addSharedProjectContribution(
                workspace = wsUpdated,
                sharedProject = bobSharedProj,
                ownerName = "Bob",
                phaseName = "Planning & Permits",
                title = "Bob Inverter Advance",
                amount = 15000.0,
                vendor = "SolarMart",
                items = emptyList()
            )
            repository.recordProjectContributor(ws.id, bobSharedProj.id, aliceUid)
        }
        val bobCats = withTimeout(FLOW_TIMEOUT_MS) { repository.observeCategories(bobUid).first { it.isNotEmpty() } }
        val bobExps = withTimeout(FLOW_TIMEOUT_MS) { repository.observeExpenses(bobUid).first { it.any { e -> e.id == bobFuelExpId } } }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyWorkspaceShares(
                uid = bobUid,
                ownerName = "Bob",
                workspace = wsUpdated,
                categories = bobCats,
                expenses = bobExps
            )
        }

        // Bob leaves workspace -> Bob keeps read-only retained vehicle & project, still sees ONLY his own expense/contribution, and can delete his own contribution
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.leaveWorkspace(ws.id)
        }
        val bobProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(bobUid).first { it != null && it.workspaceId == null }
        }!!
        assertTrue(ws.id in bobProfile.contributedWorkspaceIds)

        val bobRetained = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRetainedWorkspaceBundle(bobUid, bobProfile.contributedWorkspaceIds, null).first {
                it.vehicles.any { v -> v.id == bobSharedVeh.id } &&
                    it.projects.any { p -> p.id == bobSharedProj.id }
            }
        }
        assertTrue(bobRetained.vehicles.any { it.id == bobSharedVeh.id })
        assertTrue(bobRetained.projects.any { p -> p.id == bobSharedProj.id })
        assertTrue(bobRetained.expenses.none { it.ownerUid != bobUid })
        assertTrue(bobRetained.projectContributions.any { it.ownerUid == bobUid && it.title == "Bob Inverter Advance" })

        // Departed Bob can delete his retained contribution
        val bobContrib = bobRetained.projectContributions.first { it.ownerUid == bobUid }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteSharedProjectContribution(ws.id, bobContrib)
        }
        val bobRetainedAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRetainedWorkspaceBundle(bobUid, bobProfile.contributedWorkspaceIds, null).first {
                it.projectContributions.none { c -> c.id == bobContrib.id }
            }
        }
        assertTrue(bobRetainedAfterDelete.projectContributions.isEmpty())

        // Carol did NOT contribute -> after Carol leaves, Carol retains nothing and cannot read workspace docs
        signInTestUser(carolEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.leaveWorkspace(ws.id)
        }
        var carolDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("workspaces").document(ws.id)
                    .collection("sharedVehicles").document(bobSharedVeh.id)
                    .get().await()
            }
        } catch (e: Exception) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.unwrapFirestoreException()?.code)
            carolDenied = true
        }
        assertTrue("Non-contributor who left must receive PERMISSION_DENIED on sharedVehicles", carolDenied)

        // Cleanup
        signInTestUser(aliceEmail)
        val wsForCleanup = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkspace(ws.id).first { it != null && bobUid !in it.memberIds && carolUid !in it.memberIds }
        }!!
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteWorkspace(wsForCleanup)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
