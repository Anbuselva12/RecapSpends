package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseItem
import com.example.data.model.Project
import com.example.data.model.ProjectPayment
import com.example.data.model.ProjectPaymentItem
import com.example.data.model.ProjectPhase
import com.example.data.model.ProjectProduct
import com.example.data.model.ScannedBillDraft
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceVisit
import com.example.data.model.SharedBill
import com.example.data.model.SharedCategory
import com.example.data.model.SharedExpense
import com.example.data.model.SharedProject
import com.example.data.model.SharedProjectContribution
import com.example.data.model.SharedVehicle
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.data.model.Workspace
import com.example.util.IndianFormatters
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SelavuRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth
    private val workspaceHelper by lazy { WorkspaceRepositoryHelper(db, auth) }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun ensureUidAllowed(uid: String) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null && currentUid != uid) {
            throw SecurityException("Cross-user access is prohibited.")
        }
    }

    fun observeProfile(uid: String): Flow<UserProfile?> {
        ensureUidAllowed(uid)
        return userDoc(uid).snapshots()
            .map { snap ->
                snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.GET, "users/$uid")
                }
                throw error
            }
    }

    fun observeCategories(uid: String): Flow<List<Category>> {
        ensureUidAllowed(uid)
        return userDoc(uid).collection("categories")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Category::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sortOrder }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/categories")
                }
                throw error
            }
    }

    fun observeExpenses(uid: String): Flow<List<Expense>> {
        ensureUidAllowed(uid)
        return userDoc(uid).collection("expenses")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Expense::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateEpochMs }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/expenses")
                }
                throw error
            }
    }

    fun observeVehicles(uid: String): Flow<List<Vehicle>> {
        ensureUidAllowed(uid)
        return userDoc(uid).collection("vehicles")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Vehicle::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/vehicles")
                }
                throw error
            }
    }

    fun observeBills(uid: String): Flow<List<Bill>> {
        ensureUidAllowed(uid)
        return userDoc(uid).collection("bills")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Bill::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.dueEpochMs }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/bills")
                }
                throw error
            }
    }

    fun observeProjects(uid: String): Flow<List<Project>> {
        ensureUidAllowed(uid)
        return userDoc(uid).collection("projects")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Project::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/projects")
                }
                throw error
            }
    }

    suspend fun ensureSampleDataSeeded(uid: String, displayName: String?) {
        val currentUid = requireUserId()
        require(currentUid == uid) { "Authenticated user mismatch." }
        val profileRef = userDoc(uid)
        try {
            val snap = profileRef.get().await()
            if (!snap.exists()) {
                val batch = db.batch()
                val profile = SampleDataSeeder.defaultProfile(uid, displayName)
                batch.set(profileRef, profile.toCreateMap(uid))

                SampleDataSeeder.sampleCategories(uid).forEach { cat ->
                    batch.set(userDoc(uid).collection("categories").document(cat.id), cat.toCreateMap(uid))
                }
                batch.commit().await()
            } else {
                purgeLegacySampleDocsIfPresent(uid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            throw e
        }
    }

    private suspend fun purgeLegacySampleDocsIfPresent(uid: String) {
        val hasDemoExp = userDoc(uid).collection("expenses").document("exp_1").get().await().exists()
        val hasDemoVeh = userDoc(uid).collection("vehicles").document("v1").get().await().exists()
        val hasDemoBill = userDoc(uid).collection("bills").document("b1").get().await().exists()
        val hasDemoProj = userDoc(uid).collection("projects").document("p1").get().await().exists()
        if (!hasDemoExp && !hasDemoVeh && !hasDemoBill && !hasDemoProj) return

        val batch = db.batch()
        (1..11).forEach { idx ->
            batch.delete(userDoc(uid).collection("expenses").document("exp_$idx"))
        }
        listOf("v1", "v2").forEach { id ->
            batch.delete(userDoc(uid).collection("vehicles").document(id))
        }
        (1..10).forEach { idx ->
            batch.delete(userDoc(uid).collection("bills").document("b$idx"))
        }
        listOf("p1", "p2").forEach { id ->
            batch.delete(userDoc(uid).collection("projects").document(id))
        }
        batch.commit().await()
    }

    suspend fun restoreStarterCategories(uid: String) {
        val currentUid = requireUserId()
        require(currentUid == uid) { "Authenticated user mismatch." }
        val col = userDoc(uid).collection("categories")
        try {
            val existingIds = col.get().await().documents.map { it.id }.toSet()
            val batch = db.batch()
            var added = 0
            SampleDataSeeder.sampleCategories(uid).forEach { cat ->
                if (cat.id !in existingIds) {
                    batch.set(col.document(cat.id), cat.toCreateMap(uid))
                    added++
                }
            }
            if (added > 0) batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            throw e
        }
    }

    suspend fun updateDisplayName(newName: String) {
        val uid = requireUserId()
        val clean = newName.trim().take(80)
        require(clean.isNotEmpty()) { "Display name cannot be empty." }
        val ref = userDoc(uid)
        try {
            ref.update(
                mapOf(
                    "displayName" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteAllUserData(uid: String, deleteProfileDoc: Boolean = false) {
        val currentUid = requireUserId()
        require(currentUid == uid) { "Authenticated user mismatch." }
        try {
            val profileRef = userDoc(uid)
            val profileSnap = profileRef.get().await()
            val wsId = profileSnap.getString("workspaceId")
            if (!wsId.isNullOrBlank()) {
                if (deleteProfileDoc) {
                    workspaceHelper.leaveWorkspace(wsId)
                } else {
                    workspaceHelper.removeMySharedExpensesOnly(wsId)
                }
            }

            val docRefs = mutableListOf<com.google.firebase.firestore.DocumentReference>()
            val collections = listOf("expenses", "vehicles", "bills", "projects", "categories")
            for (name in collections) {
                val snap = userDoc(uid).collection(name).get().await()
                docRefs.addAll(snap.documents.map { it.reference })
            }
            for (chunk in docRefs.chunked(400)) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }

            if (deleteProfileDoc) {
                if (profileRef.get().await().exists()) {
                    profileRef.delete().await()
                }
            } else {
                val snap = profileRef.get().await()
                val freshDisplayName = auth.currentUser?.displayName?.trim()?.take(80)
                if (snap.exists()) {
                    val updates = mutableMapOf<String, Any>(
                        "monthlyBudget" to 65000.0,
                        "language" to "en",
                        "seeded" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!freshDisplayName.isNullOrBlank()) {
                        updates["displayName"] = freshDisplayName
                    }
                    profileRef.update(updates).await()
                } else {
                    profileRef.set(SampleDataSeeder.defaultProfile(uid, freshDisplayName).toCreateMap(uid)).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            throw e
        }
    }

    suspend fun updateLanguage(language: String) {
        val uid = requireUserId()
        val ref = userDoc(uid)
        try {
            ref.update(
                mapOf(
                    "language" to if (language == "ta") "ta" else "en",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun updateMonthlyBudget(budget: Double) {
        val uid = requireUserId()
        val ref = userDoc(uid)
        try {
            ref.update(
                mapOf(
                    "monthlyBudget" to budget.coerceIn(1000.0, 100_000_000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun saveCategory(category: Category): String {
        val uid = requireUserId()
        val id = category.id.ifBlank { "cat_${UUID.randomUUID().toString().take(8)}" }
        val ref = userDoc(uid).collection("categories").document(id)
        try {
            if (category.id.isBlank()) {
                ref.set(category.copy(id = id, userId = uid).toCreateMap(uid)).await()
            } else {
                ref.update(category.toUpdateMap()).await()
            }
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("categories").document(categoryId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    suspend fun addExpense(
        expense: Expense,
        vehicleToReset: Vehicle? = null,
        odometerReading: Int? = null,
        serviceItemIdsDone: List<String> = emptyList(),
        projectToCharge: Project? = null
    ): String {
        val uid = requireUserId()
        val expId = expense.id.ifBlank { "exp_${UUID.randomUUID().toString().take(8)}" }
        val expRef = userDoc(uid).collection("expenses").document(expId)
        try {
            val batch = db.batch()
            batch.set(expRef, expense.copy(id = expId, userId = uid).toCreateMap(uid))

            if (vehicleToReset != null && vehicleToReset.userId == uid) {
                val odoCandidate = odometerReading ?: expense.odometerReading ?: vehicleToReset.odometer
                val odo = maxOf(vehicleToReset.odometer, odoCandidate)
                val isService = serviceItemIdsDone.isNotEmpty() || expense.subcategory.equals("Service", ignoreCase = true)
                if (isService) {
                    val doneNames = mutableListOf<String>()
                    val updatedItems = vehicleToReset.items.map { item ->
                        if (item.id in serviceItemIdsDone) {
                            doneNames += item.name
                            item.copy(lastDoneEpochMs = expense.dateEpochMs, lastDoneKm = odo)
                        } else item
                    }
                    val visit = ServiceVisit(
                        dateEpochMs = expense.dateEpochMs,
                        odometer = odo,
                        workshop = expense.title.ifBlank { "Service visit" },
                        amount = expense.amount,
                        itemsDone = if (doneNames.isNotEmpty()) doneNames else listOf("General service"),
                        expenseId = expId
                    )
                    val updatedVehicle = vehicleToReset.copy(
                        odometer = odo,
                        items = updatedItems,
                        history = listOf(visit) + vehicleToReset.history
                    )
                    batch.update(
                        userDoc(uid).collection("vehicles").document(vehicleToReset.id),
                        updatedVehicle.toUpdateMap()
                    )
                } else if (odo > vehicleToReset.odometer) {
                    val updatedVehicle = vehicleToReset.copy(odometer = odo)
                    batch.update(
                        userDoc(uid).collection("vehicles").document(vehicleToReset.id),
                        updatedVehicle.toUpdateMap()
                    )
                }
            }

            if (projectToCharge != null && projectToCharge.userId == uid) {
                val updatedPhases = projectToCharge.phases.mapIndexed { idx, ph ->
                    if (idx == 0) ph.copy(spent = ph.spent + expense.amount) else ph
                }
                val newPay = ProjectPayment(
                    dateEpochMs = expense.dateEpochMs,
                    title = expense.title,
                    amount = expense.amount,
                    vendor = "—",
                    phaseName = projectToCharge.phases.firstOrNull()?.name ?: ""
                )
                val updatedProj = projectToCharge.copy(
                    phases = updatedPhases,
                    payments = listOf(newPay) + projectToCharge.payments
                )
                batch.update(
                    userDoc(uid).collection("projects").document(projectToCharge.id),
                    updatedProj.toUpdateMap()
                )
            }

            batch.commit().await()
            return expId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, expRef.path)
            throw e
        }
    }

    suspend fun updateExpense(
        expense: Expense,
        vehicleToReset: Vehicle? = null,
        odometerReading: Int? = null,
        serviceItemIdsDone: List<String> = emptyList()
    ) {
        val uid = requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID cannot be blank when updating." }
        val expRef = userDoc(uid).collection("expenses").document(expense.id)
        try {
            val batch = db.batch()
            batch.update(expRef, expense.toUpdateMap())

            if (vehicleToReset != null && vehicleToReset.userId == uid) {
                val odoCandidate = odometerReading ?: expense.odometerReading ?: vehicleToReset.odometer
                val odo = maxOf(vehicleToReset.odometer, odoCandidate)
                val isService = serviceItemIdsDone.isNotEmpty()
                if (isService) {
                    val doneNames = mutableListOf<String>()
                    val updatedItems = vehicleToReset.items.map { item ->
                        if (item.id in serviceItemIdsDone) {
                            doneNames += item.name
                            item.copy(lastDoneEpochMs = expense.dateEpochMs, lastDoneKm = odo)
                        } else item
                    }
                    val updatedVehicle = vehicleToReset.copy(
                        odometer = odo,
                        items = updatedItems
                    )
                    batch.update(
                        userDoc(uid).collection("vehicles").document(vehicleToReset.id),
                        updatedVehicle.toUpdateMap()
                    )
                } else if (odo > vehicleToReset.odometer) {
                    val updatedVehicle = vehicleToReset.copy(odometer = odo)
                    batch.update(
                        userDoc(uid).collection("vehicles").document(vehicleToReset.id),
                        updatedVehicle.toUpdateMap()
                    )
                }
            }

            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, expRef.path)
            throw e
        }
    }

    suspend fun addVehicle(
        name: String,
        type: String,
        plate: String,
        odometer: Int,
        avgMileage: Double,
        hiddenFromWorkspace: Boolean = false
    ): String {
        val uid = requireUserId()
        val id = "v_${UUID.randomUUID().toString().take(8)}"
        val nowMs = System.currentTimeMillis()
        val template = if (type == "car") SampleDataSeeder.carDefaultTemplate else SampleDataSeeder.bikeDefaultTemplate
        val items = template.map { it.copy(lastDoneEpochMs = nowMs, lastDoneKm = odometer) }
        val short = name.trim().split(" ").takeLast(2).joinToString(" ").ifBlank { name.trim() }
        val vehicle = Vehicle(
            id = id,
            userId = uid,
            name = name.trim(),
            shortName = short,
            type = type,
            plate = plate.ifBlank { "Add plate" },
            odometer = odometer,
            avgMileage = avgMileage,
            items = items,
            history = emptyList(),
            hiddenFromWorkspace = hiddenFromWorkspace
        )
        val ref = userDoc(uid).collection("vehicles").document(id)
        try {
            ref.set(vehicle.toCreateMap(uid)).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("vehicles").document(vehicle.id)
        try {
            ref.update(vehicle.toUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun addBill(bill: Bill): String {
        val uid = requireUserId()
        val id = "b_${UUID.randomUUID().toString().take(8)}"
        val ref = userDoc(uid).collection("bills").document(id)
        try {
            ref.set(bill.copy(id = id, userId = uid).toCreateMap(uid)).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun updateBill(bill: Bill) {
        val uid = requireUserId()
        require(bill.id.isNotBlank()) { "Bill ID cannot be blank when updating." }
        val ref = userDoc(uid).collection("bills").document(bill.id)
        try {
            ref.update(bill.toUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteBill(billId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("bills").document(billId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    suspend fun markBillPaid(bill: Bill): Long? {
        val uid = requireUserId()
        val nowMs = System.currentTimeMillis()
        val billRef = userDoc(uid).collection("bills").document(bill.id)
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"
        val expRef = userDoc(uid).collection("expenses").document(expId)
        try {
            val batch = db.batch()
            batch.update(
                billRef,
                mapOf(
                    "paid" to true,
                    "paidOnEpochMs" to nowMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val exp = Expense(
                id = expId,
                userId = uid,
                title = bill.name,
                amount = bill.amount,
                dateEpochMs = nowMs,
                categoryId = bill.categoryId,
                subcategory = bill.subcategory,
                paymentMode = "UPI"
            )
            batch.set(expRef, exp.toCreateMap(uid))

            val repeatMonths = when (bill.repeat) {
                "Monthly" -> 1
                "Bi-monthly" -> 2
                "Half-yearly" -> 6
                "Yearly" -> 12
                else -> 0
            }
            var nextDueMs: Long? = null
            if (repeatMonths > 0) {
                nextDueMs = IndianFormatters.addMonths(bill.dueEpochMs, repeatMonths)
                val nextId = "b_${UUID.randomUUID().toString().take(8)}"
                val nextBill = bill.copy(
                    id = nextId,
                    userId = uid,
                    dueEpochMs = nextDueMs,
                    paid = false,
                    paidOnEpochMs = null
                )
                batch.set(userDoc(uid).collection("bills").document(nextId), nextBill.toCreateMap(uid))
            }

            batch.commit().await()
            return nextDueMs
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, billRef.path)
            throw e
        }
    }

    suspend fun addProject(
        name: String,
        totalBudget: Double,
        templateName: String,
        hiddenFromWorkspace: Boolean = false
    ): String {
        val uid = requireUserId()
        val id = "p_${UUID.randomUUID().toString().take(8)}"
        val splits = SampleDataSeeder.projectTemplates[templateName]
            ?: SampleDataSeeder.projectTemplates["House build"]!!
        val phases = splits.map { (phaseName, pct) ->
            ProjectPhase(
                name = phaseName,
                budget = kotlin.math.round(totalBudget * pct / 100.0),
                spent = 0.0
            )
        }
        val isBiz = templateName == "Business setup"
        val project = Project(
            id = id,
            userId = uid,
            name = name.trim(),
            subtitle = "$templateName · ${phases.size} phases",
            iconKey = if (isBiz) "building" else "house",
            colorIndex = if (isBiz) 7 else 2,
            budget = totalBudget,
            phases = phases,
            vendors = emptyList(),
            payments = emptyList(),
            productCatalog = Project.defaultProjectProducts,
            hiddenFromWorkspace = hiddenFromWorkspace
        )
        val ref = userDoc(uid).collection("projects").document(id)
        try {
            ref.set(project.toCreateMap(uid)).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun updateProject(project: Project) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("projects").document(project.id)
        try {
            ref.update(project.toUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun updateProjectProducts(project: Project, products: List<ProjectProduct>) {
        updateProject(project.copy(productCatalog = products))
    }

    suspend fun addProjectPayment(
        project: Project,
        phaseIndex: Int,
        title: String,
        amount: Double,
        vendorName: String = "—",
        items: List<ProjectPaymentItem> = emptyList(),
        updatedProductCatalog: List<ProjectProduct>? = null
    ) {
        val uid = requireUserId()
        val nowMs = System.currentTimeMillis()
        val safeIdx = phaseIndex.coerceIn(0, (project.phases.size - 1).coerceAtLeast(0))
        val phaseName = project.phases.getOrNull(safeIdx)?.name ?: ""
        val updatedPhases = project.phases.mapIndexed { idx, ph ->
            if (idx == safeIdx) ph.copy(spent = ph.spent + amount) else ph
        }
        val updatedVendors = project.vendors.map { v ->
            if (v.name.equals(vendorName, ignoreCase = true)) v.copy(paid = v.paid + amount) else v
        }
        val cleanItems = items.filter { it.name.isNotBlank() && it.qty > 0.0 }
        val newPayment = ProjectPayment(
            dateEpochMs = nowMs,
            title = title.ifBlank { "Payment" },
            amount = amount,
            vendor = vendorName.ifBlank { "—" },
            phaseName = phaseName,
            items = cleanItems
        )
        val updatedProject = project.copy(
            phases = updatedPhases,
            vendors = updatedVendors,
            payments = listOf(newPayment) + project.payments,
            productCatalog = updatedProductCatalog ?: project.productCatalog
        )
        val projRef = userDoc(uid).collection("projects").document(project.id)
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"
        val expRef = userDoc(uid).collection("expenses").document(expId)
        try {
            val batch = db.batch()
            batch.update(projRef, updatedProject.toUpdateMap())
            val mappedExpenseItems = cleanItems.map { item ->
                ExpenseItem(
                    name = item.name,
                    qty = item.qty,
                    unit = item.unit,
                    rate = item.unitPrice,
                    amount = item.totalPrice
                )
            }.takeIf { it.isNotEmpty() }
            val expense = Expense(
                id = expId,
                userId = uid,
                title = title.ifBlank { "Payment" },
                amount = amount,
                dateEpochMs = nowMs,
                categoryId = if (project.colorIndex == 7) "biz" else "home",
                subcategory = phaseName,
                paymentMode = "Bank",
                projectId = project.id,
                items = mappedExpenseItems
            )
            batch.set(expRef, expense.toCreateMap(uid))
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, projRef.path)
            throw e
        }
    }

    suspend fun saveScannedBill(
        draft: ScannedBillDraft,
        selectedVehicle: Vehicle?
    ): Int {
        val uid = requireUserId()
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"
        val expRef = userDoc(uid).collection("expenses").document(expId)
        try {
            val batch = db.batch()
            val expenseItems = if (draft.keepItemLevel) {
                draft.items.flatMap { item ->
                    if (item.subItems.isNotEmpty()) {
                        listOf(
                            ExpenseItem(
                                name = item.name,
                                qty = item.qty,
                                unit = item.unit,
                                rate = item.taxableAmount,
                                discount = item.discount,
                                hsn = item.hsn,
                                gstRate = item.gstRate,
                                amount = item.taxableAmount * (1.0 + item.gstRate / 100.0)
                            )
                        ) + item.subItems.map { sub ->
                            ExpenseItem(
                                name = sub.name,
                                qty = sub.qty,
                                unit = "pcs",
                                rate = sub.rate,
                                hsn = item.hsn,
                                gstRate = item.gstRate,
                                parentItemName = item.name,
                                amount = sub.lineAmount
                            )
                        }
                    } else {
                        listOf(
                            ExpenseItem(
                                name = item.name,
                                qty = item.qty,
                                unit = item.unit,
                                rate = item.rate,
                                discount = item.discount,
                                hsn = item.hsn,
                                gstRate = item.gstRate,
                                amount = item.taxableAmount * (1.0 + item.gstRate / 100.0)
                            )
                        )
                    }
                }
            } else null

            val expense = Expense(
                id = expId,
                userId = uid,
                title = draft.merchant.ifBlank { "Scanned bill" },
                amount = draft.grandTotal,
                dateEpochMs = draft.dateEpochMs,
                categoryId = draft.suggestedCategoryId,
                subcategory = draft.suggestedSubcategory,
                paymentMode = "UPI",
                vehicleId = selectedVehicle?.id,
                gstin = draft.gstin.takeIf { it.isNotBlank() },
                billNumber = draft.billNumber.takeIf { it.isNotBlank() },
                taxableValue = draft.taxableTotal,
                cgst = draft.cgst,
                sgst = draft.sgst,
                igst = draft.igst,
                roundOff = draft.roundOff,
                items = expenseItems
            )
            batch.set(expRef, expense.toCreateMap(uid))

            var resetCount = 0
            if (selectedVehicle != null && selectedVehicle.userId == uid && draft.suggestedCategoryId == "veh") {
                val mappedIds = draft.items.mapNotNull { it.mappedServiceItemId }.toSet()
                val updatedItems = selectedVehicle.items.map { svc ->
                    if (svc.id in mappedIds) {
                        resetCount++
                        svc.copy(
                            lastDoneEpochMs = draft.dateEpochMs,
                            lastDoneKm = selectedVehicle.odometer
                        )
                    } else svc
                }
                val visit = ServiceVisit(
                    dateEpochMs = draft.dateEpochMs,
                    odometer = selectedVehicle.odometer,
                    workshop = draft.merchant,
                    amount = draft.grandTotal,
                    itemsDone = draft.items.map { it.name.replace(Regex("\\s*\\(.*\\)"), "") },
                    expenseId = expId
                )
                val updatedVehicle = selectedVehicle.copy(
                    items = updatedItems,
                    history = listOf(visit) + selectedVehicle.history
                )
                batch.update(
                    userDoc(uid).collection("vehicles").document(selectedVehicle.id),
                    updatedVehicle.toUpdateMap()
                )
            }

            batch.commit().await()
            return resetCount
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, expRef.path)
            throw e
        }
    }

    suspend fun updateExpensePrivacy(expenseId: String, isPrivate: Boolean) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("expenses").document(expenseId)
        try {
            ref.update(
                mapOf(
                    "isPrivate" to isPrivate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("expenses").document(expenseId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- Shared Family Workspace Delegation ---

    fun observeWorkspace(workspaceId: String?): Flow<Workspace?> =
        workspaceHelper.observeWorkspace(workspaceId)

    fun observeSharedExpenses(workspaceId: String?, uid: String): Flow<List<SharedExpense>> =
        workspaceHelper.observeSharedExpenses(workspaceId, uid)

    fun observeSharedVehicles(workspaceId: String?, uid: String): Flow<List<SharedVehicle>> =
        workspaceHelper.observeSharedVehicles(workspaceId, uid)

    fun observeSharedProjects(workspaceId: String?, uid: String): Flow<List<SharedProject>> =
        workspaceHelper.observeSharedProjects(workspaceId, uid)

    fun observeSharedProjectContributions(workspaceId: String?, uid: String): Flow<List<SharedProjectContribution>> =
        workspaceHelper.observeSharedProjectContributions(workspaceId, uid)

    fun observeSharedBills(workspaceId: String?, uid: String): Flow<List<SharedBill>> =
        workspaceHelper.observeSharedBills(workspaceId, uid)

    fun observeSharedCategories(workspaceId: String?, uid: String): Flow<List<SharedCategory>> =
        workspaceHelper.observeSharedCategories(workspaceId, uid)

    fun observeRetainedWorkspaceBundle(
        uid: String,
        contributedWorkspaceIds: List<String>,
        activeWorkspaceId: String?
    ): Flow<RetainedWorkspaceBundle> =
        workspaceHelper.observeRetainedWorkspaceBundle(uid, contributedWorkspaceIds, activeWorkspaceId)

    suspend fun recordVehicleContributor(workspaceId: String, sharedVehicleId: String, vehicleOwnerUid: String) =
        workspaceHelper.recordVehicleContributor(workspaceId, sharedVehicleId, vehicleOwnerUid)

    suspend fun recordProjectContributor(workspaceId: String, sharedProjectId: String, projectOwnerUid: String) =
        workspaceHelper.recordProjectContributor(workspaceId, sharedProjectId, projectOwnerUid)

    suspend fun addSharedProjectContribution(
        workspace: Workspace,
        sharedProject: SharedProject,
        ownerName: String,
        phaseName: String,
        title: String,
        amount: Double,
        vendor: String,
        items: List<ProjectPaymentItem>
    ): String {
        val uid = requireUserId()
        val expId = "exp_${UUID.randomUUID().toString().take(8)}"
        val cleanItems = items.filter { it.name.isNotBlank() && it.qty > 0.0 }
        val mappedExpenseItems = cleanItems.map { item ->
            ExpenseItem(
                name = item.name,
                qty = item.qty,
                unit = item.unit,
                rate = item.unitPrice,
                amount = item.totalPrice
            )
        }.takeIf { it.isNotEmpty() }
        val exp = Expense(
            id = expId,
            userId = uid,
            title = title.ifBlank { "Payment" },
            amount = amount,
            dateEpochMs = System.currentTimeMillis(),
            categoryId = if (sharedProject.colorIndex == 7) "biz" else "home",
            subcategory = phaseName,
            paymentMode = "Bank",
            projectId = sharedProject.id,
            items = mappedExpenseItems
        )
        userDoc(uid).collection("expenses").document(expId).set(exp.toCreateMap(uid)).await()
        return workspaceHelper.addSharedProjectContribution(
            workspace = workspace,
            sharedProject = sharedProject,
            ownerName = ownerName,
            title = title,
            amount = amount,
            vendor = vendor,
            phaseName = phaseName,
            items = cleanItems,
            linkedExpenseId = expId
        )
    }

    suspend fun deleteSharedProjectContribution(workspaceId: String, contribution: SharedProjectContribution) =
        workspaceHelper.deleteSharedProjectContribution(workspaceId, contribution)

    suspend fun deleteRetainedSharedExpense(sharedExpense: SharedExpense) =
        workspaceHelper.deleteRetainedSharedExpense(sharedExpense)

    suspend fun createWorkspace(name: String, ownerName: String): Workspace =
        workspaceHelper.createWorkspace(name, ownerName)

    suspend fun joinWorkspaceByCode(inviteCode: String, joinerName: String): String =
        workspaceHelper.joinWorkspaceByCode(inviteCode, joinerName)

    suspend fun regenerateInviteCode(workspace: Workspace, ownerName: String): String =
        workspaceHelper.regenerateInviteCode(workspace, ownerName)

    suspend fun removeMemberFromWorkspace(workspace: Workspace, targetUid: String) =
        workspaceHelper.removeMemberFromWorkspace(workspace, targetUid)

    suspend fun leaveWorkspace(workspaceId: String) =
        workspaceHelper.leaveWorkspace(workspaceId)

    suspend fun deleteWorkspace(workspace: Workspace) =
        workspaceHelper.deleteWorkspace(workspace)

    suspend fun clearUserWorkspaceId(uid: String) =
        workspaceHelper.clearUserWorkspaceId(uid)

    suspend fun syncMyWorkspaceShares(
        uid: String,
        ownerName: String,
        workspace: Workspace,
        categories: List<Category>,
        expenses: List<Expense>,
        vehicles: List<Vehicle> = emptyList(),
        bills: List<Bill> = emptyList(),
        projects: List<Project> = emptyList(),
        existingSharedByMe: List<SharedExpense>? = null,
        existingSharedVehicles: List<SharedVehicle>? = null,
        existingSharedProjects: List<SharedProject>? = null,
        existingSharedBills: List<SharedBill>? = null,
        existingSharedCategories: List<SharedCategory>? = null,
        existingMyContributions: List<SharedProjectContribution>? = null
    ) = workspaceHelper.syncMyWorkspaceShares(
        uid = uid,
        ownerName = ownerName,
        workspace = workspace,
        categories = categories,
        expenses = expenses,
        vehicles = vehicles,
        bills = bills,
        projects = projects,
        existingSharedByMe = existingSharedByMe,
        existingSharedVehicles = existingSharedVehicles,
        existingSharedProjects = existingSharedProjects,
        existingSharedBills = existingSharedBills,
        existingSharedCategories = existingSharedCategories,
        existingMyContributions = existingMyContributions
    )
}

