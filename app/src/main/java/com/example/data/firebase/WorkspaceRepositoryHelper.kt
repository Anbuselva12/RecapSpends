package com.example.data.firebase

import android.util.Log
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.Project
import com.example.data.model.ProjectPaymentItem
import com.example.data.model.SharedBill
import com.example.data.model.SharedCategory
import com.example.data.model.SharedExpense
import com.example.data.model.SharedProject
import com.example.data.model.SharedProjectContribution
import com.example.data.model.SharedVehicle
import com.example.data.model.Vehicle
import com.example.data.model.Workspace
import com.example.data.model.WorkspaceInvite
import com.example.data.model.WorkspaceMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

data class RetainedWorkspaceBundle(
    val vehicles: List<SharedVehicle> = emptyList(),
    val projects: List<SharedProject> = emptyList(),
    val expenses: List<SharedExpense> = emptyList(),
    val projectContributions: List<SharedProjectContribution> = emptyList()
)

class WorkspaceRepositoryHelper(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing workspaces.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun workspaceDoc(workspaceId: String) = db.collection("workspaces").document(workspaceId)
    private fun inviteDoc(code: String) = db.collection("workspaceInvites").document(code)

    private fun Throwable.isPermissionDenied(): Boolean =
        generateSequence(this) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    fun observeWorkspace(workspaceId: String?): Flow<Workspace?> {
        if (workspaceId.isNullOrBlank()) return flowOf(null)
        return workspaceDoc(workspaceId).snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    Workspace::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) {
                    Log.w(TAG, "No workspace access for $workspaceId (removed or dissolved); returning null")
                    emit(null)
                } else {
                    throw error
                }
            }
    }

    fun observeSharedExpenses(workspaceId: String?, uid: String): Flow<List<SharedExpense>> {
        if (workspaceId.isNullOrBlank()) return flowOf(emptyList())
        return workspaceDoc(workspaceId)
            .collection("expenses")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SharedExpense::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateEpochMs }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) {
                    Log.w(TAG, "No shared expense access for $workspaceId; returning empty list")
                    emit(emptyList())
                } else {
                    throw error
                }
            }
    }

    fun observeSharedVehicles(workspaceId: String?, uid: String): Flow<List<SharedVehicle>> {
        if (workspaceId.isNullOrBlank()) return flowOf(emptyList())
        return workspaceDoc(workspaceId)
            .collection("vehicles")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SharedVehicle::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) emit(emptyList()) else throw error
            }
    }

    fun observeSharedProjects(workspaceId: String?, uid: String): Flow<List<SharedProject>> {
        if (workspaceId.isNullOrBlank()) return flowOf(emptyList())
        return workspaceDoc(workspaceId)
            .collection("projects")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SharedProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) emit(emptyList()) else throw error
            }
    }

    fun observeSharedProjectContributions(
        workspaceId: String?,
        uid: String
    ): Flow<List<SharedProjectContribution>> {
        if (workspaceId.isNullOrBlank()) return flowOf(emptyList())
        return workspaceDoc(workspaceId)
            .collection("projectContributions")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SharedProjectContribution::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateEpochMs }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) emit(emptyList()) else throw error
            }
    }

    fun observeSharedBills(workspaceId: String?, uid: String): Flow<List<SharedBill>> {
        if (workspaceId.isNullOrBlank()) return flowOf(emptyList())
        return workspaceDoc(workspaceId)
            .collection("bills")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SharedBill::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.dueEpochMs }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) emit(emptyList()) else throw error
            }
    }

    fun observeSharedCategories(workspaceId: String?, uid: String): Flow<List<SharedCategory>> {
        if (workspaceId.isNullOrBlank()) return flowOf(emptyList())
        return workspaceDoc(workspaceId)
            .collection("categories")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    SharedCategory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sortOrder }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (error.isPermissionDenied()) emit(emptyList()) else throw error
            }
    }

    fun observeRetainedWorkspaceBundle(
        uid: String,
        contributedWorkspaceIds: List<String>,
        activeWorkspaceId: String?
    ): Flow<RetainedWorkspaceBundle> {
        val pastWsIds = contributedWorkspaceIds
            .filter { it.isNotBlank() && it != activeWorkspaceId }
            .distinct()
        if (pastWsIds.isEmpty()) return flowOf(RetainedWorkspaceBundle())

        val perWorkspaceFlows = pastWsIds.map { wsId ->
            val vehiclesFlow = workspaceDoc(wsId)
                .collection("vehicles")
                .whereArrayContains("contributorIds", uid)
                .snapshots()
                .map { it.toObjects(SharedVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is CancellationException && e.cause == null) throw e
                    if (e.isPermissionDenied()) emit(emptyList()) else throw e
                }
            val projectsFlow = workspaceDoc(wsId)
                .collection("projects")
                .whereArrayContains("contributorIds", uid)
                .snapshots()
                .map { it.toObjects(SharedProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is CancellationException && e.cause == null) throw e
                    if (e.isPermissionDenied()) emit(emptyList()) else throw e
                }
            val expensesFlow = workspaceDoc(wsId)
                .collection("expenses")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SharedExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.ownerUid == uid }
                }
                .catch { e ->
                    if (e is CancellationException && e.cause == null) throw e
                    if (e.isPermissionDenied()) emit(emptyList()) else throw e
                }
            val contribFlow = workspaceDoc(wsId)
                .collection("projectContributions")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SharedProjectContribution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.ownerUid == uid }
                }
                .catch { e ->
                    if (e is CancellationException && e.cause == null) throw e
                    if (e.isPermissionDenied()) emit(emptyList()) else throw e
                }
            combine(vehiclesFlow, projectsFlow, expensesFlow, contribFlow) { v, p, e, c ->
                RetainedWorkspaceBundle(
                    vehicles = v.filter { it.ownerUid != uid },
                    projects = p.filter { it.ownerUid != uid },
                    expenses = e,
                    projectContributions = c
                )
            }
        }

        return combine(perWorkspaceFlows) { bundles ->
            RetainedWorkspaceBundle(
                vehicles = bundles.flatMap { it.vehicles }.distinctBy { "${it.workspaceId}_${it.id}" },
                projects = bundles.flatMap { it.projects }.distinctBy { "${it.workspaceId}_${it.id}" },
                expenses = bundles.flatMap { it.expenses }.sortedByDescending { it.dateEpochMs },
                projectContributions = bundles.flatMap { it.projectContributions }.sortedByDescending { it.dateEpochMs }
            )
        }
    }

    suspend fun createWorkspace(name: String, ownerName: String): Workspace {
        val uid = requireUid()
        val cleanName = name.trim().ifBlank { "Family Workspace" }.take(80)
        val cleanOwner = ownerName.trim().ifBlank { "Owner" }.take(80)
        val workspaceId = "ws_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val code = generateInviteCode()

        val ws = Workspace(
            id = workspaceId,
            name = cleanName,
            ownerUid = uid,
            inviteCode = code,
            memberIds = listOf(uid),
            members = mapOf(
                uid to WorkspaceMember(
                    uid = uid,
                    name = cleanOwner,
                    role = "owner",
                    joinedAtMs = System.currentTimeMillis()
                )
            )
        )
        val invite = WorkspaceInvite(
            code = code,
            workspaceId = workspaceId,
            workspaceName = cleanName,
            ownerUid = uid,
            ownerName = cleanOwner
        )

        val batch = db.batch()
        batch.set(workspaceDoc(workspaceId), ws.toCreateMap(uid, cleanOwner))
        batch.set(inviteDoc(code), invite.toCreateMap())
        batch.update(
            userDoc(uid),
            mapOf(
                "workspaceId" to workspaceId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        return ws
    }

    suspend fun joinWorkspaceByCode(inviteCode: String, joinerName: String): String {
        val uid = requireUid()
        val cleanCode = inviteCode.trim().uppercase().replace(" ", "")
        require(cleanCode.length in 6..24 && cleanCode.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            "Enter a valid invite code (6–24 characters)."
        }
        val cleanName = joinerName.trim().ifBlank { "Family Member" }.take(80)

        val invSnap = inviteDoc(cleanCode).get().await()
        if (!invSnap.exists()) {
            throw IllegalArgumentException("Invite code \"$cleanCode\" was not found or has expired.")
        }
        val workspaceId = invSnap.getString("workspaceId")
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Invite code is missing workspace reference.")

        val currentProfileWs = userDoc(uid).get().await().getString("workspaceId")
        if (!currentProfileWs.isNullOrBlank() && currentProfileWs != workspaceId) {
            throw IllegalStateException("Please leave your current workspace before joining a new one.")
        }
        if (currentProfileWs == workspaceId) {
            return workspaceId
        }

        val member = WorkspaceMember(
            uid = uid,
            name = cleanName,
            role = "member",
            joinedAtMs = System.currentTimeMillis()
        )
        try {
            workspaceDoc(workspaceId).update(
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(uid),
                    "members.$uid" to member.toMap(),
                    "lastJoinCode" to cleanCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw IllegalStateException("Could not join workspace. It may be full (max 10 members) or the invite changed.")
            }
            throw e
        }

        userDoc(uid).update(
            mapOf(
                "workspaceId" to workspaceId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        return workspaceId
    }

    suspend fun regenerateInviteCode(workspace: Workspace, ownerName: String): String {
        val uid = requireUid()
        require(workspace.ownerUid == uid) { "Only the workspace creator can regenerate the invite code." }
        val newCode = generateInviteCode()
        val invite = WorkspaceInvite(
            code = newCode,
            workspaceId = workspace.id,
            workspaceName = workspace.name,
            ownerUid = uid,
            ownerName = ownerName.trim().ifBlank { "Owner" }.take(80)
        )
        val batch = db.batch()
        if (workspace.inviteCode.isNotBlank()) {
            batch.delete(inviteDoc(workspace.inviteCode))
        }
        batch.set(inviteDoc(newCode), invite.toCreateMap())
        batch.update(
            workspaceDoc(workspace.id),
            mapOf(
                "inviteCode" to newCode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        return newCode
    }

    suspend fun recordVehicleContributor(workspaceId: String, sharedVehicleId: String, vehicleOwnerUid: String) {
        val uid = requireUid()
        if (vehicleOwnerUid == uid || workspaceId.isBlank() || sharedVehicleId.isBlank()) return
        try {
            workspaceDoc(workspaceId).collection("vehicles").document(sharedVehicleId)
                .update(
                    mapOf(
                        "contributorIds" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            userDoc(uid).update(
                mapOf(
                    "contributedWorkspaceIds" to FieldValue.arrayUnion(workspaceId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                e.code != FirebaseFirestoreException.Code.NOT_FOUND
            ) {
                throw e
            }
        }
    }

    suspend fun recordProjectContributor(workspaceId: String, sharedProjectId: String, projectOwnerUid: String) {
        val uid = requireUid()
        if (projectOwnerUid == uid || workspaceId.isBlank() || sharedProjectId.isBlank()) return
        try {
            workspaceDoc(workspaceId).collection("projects").document(sharedProjectId)
                .update(
                    mapOf(
                        "contributorIds" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            userDoc(uid).update(
                mapOf(
                    "contributedWorkspaceIds" to FieldValue.arrayUnion(workspaceId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                e.code != FirebaseFirestoreException.Code.NOT_FOUND
            ) {
                throw e
            }
        }
    }

    suspend fun addSharedProjectContribution(
        workspace: Workspace,
        sharedProject: SharedProject,
        ownerName: String,
        title: String,
        amount: Double,
        vendor: String,
        phaseName: String,
        items: List<ProjectPaymentItem>,
        linkedExpenseId: String
    ): String {
        val uid = requireUid()
        val contribId = "pcon_${UUID.randomUUID().toString().take(8)}"
        val contrib = SharedProjectContribution(
            id = contribId,
            workspaceId = workspace.id,
            sharedProjectId = sharedProject.id,
            projectOwnerUid = sharedProject.ownerUid,
            ownerUid = uid,
            ownerName = ownerName.ifBlank { "Member" }.take(80),
            memberIds = workspace.memberIds.take(10),
            dateEpochMs = System.currentTimeMillis(),
            title = title.ifBlank { "Payment" },
            amount = amount,
            vendor = vendor.ifBlank { "—" },
            phaseName = phaseName,
            items = items,
            linkedExpenseId = linkedExpenseId.takeIf { it.isNotBlank() },
            retainedForDeparture = false
        )
        workspaceDoc(workspace.id).collection("projectContributions").document(contribId)
            .set(contrib.toCreateMap()).await()
        if (sharedProject.ownerUid != uid) {
            recordProjectContributor(workspace.id, sharedProject.id, sharedProject.ownerUid)
        }
        return contribId
    }

    suspend fun deleteSharedProjectContribution(
        workspaceId: String,
        contribution: SharedProjectContribution
    ) {
        val uid = requireUid()
        workspaceDoc(workspaceId).collection("projectContributions").document(contribution.id)
            .delete().await()
        val linkedExp = contribution.linkedExpenseId
        if (!linkedExp.isNullOrBlank() && contribution.ownerUid == uid) {
            try {
                userDoc(uid).collection("expenses").document(linkedExp).delete().await()
            } catch (_: Exception) {
            }
            try {
                workspaceDoc(workspaceId).collection("expenses").document("${uid}_${linkedExp}").delete().await()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun deleteRetainedSharedExpense(sharedExpense: SharedExpense) {
        val uid = requireUid()
        if (sharedExpense.ownerUid != uid) return
        workspaceDoc(sharedExpense.workspaceId)
            .collection("expenses")
            .document(sharedExpense.id)
            .delete().await()
        if (sharedExpense.sourceExpenseId.isNotBlank()) {
            try {
                userDoc(uid).collection("expenses").document(sharedExpense.sourceExpenseId).delete().await()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun removeMemberFromWorkspace(workspace: Workspace, targetUid: String) {
        val uid = requireUid()
        require(workspace.ownerUid == uid) { "Only the workspace creator can remove members." }
        require(targetUid != uid) { "Use Delete workspace to remove yourself as creator." }

        val wsRef = workspaceDoc(workspace.id)
        val expensesSnap = wsRef.collection("expenses").whereArrayContains("memberIds", uid).get().await()
        val contribSnap = wsRef.collection("projectContributions").whereArrayContains("memberIds", uid).get().await()
        val vehiclesSnap = wsRef.collection("vehicles").whereArrayContains("memberIds", uid).get().await()
        val projectsSnap = wsRef.collection("projects").whereArrayContains("memberIds", uid).get().await()
        val billsSnap = wsRef.collection("bills").whereArrayContains("memberIds", uid).get().await()
        val categoriesSnap = wsRef.collection("categories").whereArrayContains("memberIds", uid).get().await()

        val ops = mutableListOf<(WriteBatch) -> Unit>()

        for (doc in expensesSnap.documents) {
            if (doc.getString("ownerUid") == targetUid) {
                val foreignVehOwner = doc.getString("sharedVehicleOwnerUid")?.takeIf { it.isNotBlank() && it != targetUid }
                val foreignProjOwner = doc.getString("sharedProjectOwnerUid")?.takeIf { it.isNotBlank() && it != targetUid }
                if (foreignVehOwner != null || foreignProjOwner != null) {
                    ops.add { batch ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberIds" to listOf(targetUid),
                                "retainedForDeparture" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    ops.add { batch -> batch.delete(doc.reference) }
                }
            } else {
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (doc in contribSnap.documents) {
            if (doc.getString("ownerUid") == targetUid) {
                val foreignProjOwner = doc.getString("projectOwnerUid")?.takeIf { it.isNotBlank() && it != targetUid }
                if (foreignProjOwner != null) {
                    ops.add { batch ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberIds" to listOf(targetUid),
                                "retainedForDeparture" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    ops.add { batch -> batch.delete(doc.reference) }
                }
            } else {
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (doc in vehiclesSnap.documents) {
            if (doc.getString("ownerUid") == targetUid) {
                val otherContribs = (doc.get("contributorIds") as? List<*>).orEmpty()
                    .filterIsInstance<String>()
                    .filter { it != targetUid }
                if (otherContribs.isEmpty()) {
                    ops.add { batch -> batch.delete(doc.reference) }
                } else {
                    ops.add { batch ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberIds" to FieldValue.arrayRemove(targetUid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            } else {
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (doc in projectsSnap.documents) {
            if (doc.getString("ownerUid") == targetUid) {
                val otherContribs = (doc.get("contributorIds") as? List<*>).orEmpty()
                    .filterIsInstance<String>()
                    .filter { it != targetUid }
                if (otherContribs.isEmpty()) {
                    ops.add { batch -> batch.delete(doc.reference) }
                } else {
                    ops.add { batch ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberIds" to FieldValue.arrayRemove(targetUid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            } else {
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (doc in billsSnap.documents) {
            if (doc.getString("ownerUid") == targetUid) {
                ops.add { batch -> batch.delete(doc.reference) }
            } else {
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (doc in categoriesSnap.documents) {
            if (doc.getString("ownerUid") == targetUid) {
                ops.add { batch -> batch.delete(doc.reference) }
            } else {
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "memberIds" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        ops.add { batch ->
            batch.update(
                wsRef,
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(targetUid),
                    "members.$targetUid" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        commitBatchedOps(ops)
    }

    suspend fun leaveWorkspace(workspaceId: String) {
        val uid = requireUid()
        try {
            val wsRef = workspaceDoc(workspaceId)
            val wsSnap = wsRef.get().await()
            if (wsSnap.exists()) {
                val ownerUid = wsSnap.getString("ownerUid") ?: ""
                if (ownerUid == uid) {
                    val ws = wsSnap.toObject(Workspace::class.java)
                    if (ws != null) {
                        deleteWorkspace(ws)
                        return
                    }
                } else {
                    val expensesSnap = wsRef.collection("expenses").whereArrayContains("memberIds", uid).get().await()
                    val contribSnap = wsRef.collection("projectContributions").whereArrayContains("memberIds", uid).get().await()
                    val vehiclesSnap = wsRef.collection("vehicles").whereArrayContains("memberIds", uid).get().await()
                    val projectsSnap = wsRef.collection("projects").whereArrayContains("memberIds", uid).get().await()
                    val billsSnap = wsRef.collection("bills").whereArrayContains("memberIds", uid).get().await()
                    val categoriesSnap = wsRef.collection("categories").whereArrayContains("memberIds", uid).get().await()

                    val ops = mutableListOf<(WriteBatch) -> Unit>()

                    for (doc in expensesSnap.documents) {
                        if (doc.getString("ownerUid") == uid) {
                            val foreignVehOwner = doc.getString("sharedVehicleOwnerUid")?.takeIf { it.isNotBlank() && it != uid }
                            val foreignProjOwner = doc.getString("sharedProjectOwnerUid")?.takeIf { it.isNotBlank() && it != uid }
                            if (foreignVehOwner != null || foreignProjOwner != null) {
                                ops.add { batch ->
                                    batch.update(
                                        doc.reference,
                                        mapOf(
                                            "memberIds" to listOf(uid),
                                            "retainedForDeparture" to true,
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                }
                            } else {
                                ops.add { batch -> batch.delete(doc.reference) }
                            }
                        } else {
                            ops.add { batch ->
                                batch.update(
                                    doc.reference,
                                    mapOf(
                                        "memberIds" to FieldValue.arrayRemove(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    for (doc in contribSnap.documents) {
                        if (doc.getString("ownerUid") == uid) {
                            val foreignProjOwner = doc.getString("projectOwnerUid")?.takeIf { it.isNotBlank() && it != uid }
                            if (foreignProjOwner != null) {
                                ops.add { batch ->
                                    batch.update(
                                        doc.reference,
                                        mapOf(
                                            "memberIds" to listOf(uid),
                                            "retainedForDeparture" to true,
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                }
                            } else {
                                ops.add { batch -> batch.delete(doc.reference) }
                            }
                        } else {
                            ops.add { batch ->
                                batch.update(
                                    doc.reference,
                                    mapOf(
                                        "memberIds" to FieldValue.arrayRemove(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    for (doc in vehiclesSnap.documents) {
                        if (doc.getString("ownerUid") == uid) {
                            val otherContribs = (doc.get("contributorIds") as? List<*>).orEmpty()
                                .filterIsInstance<String>()
                                .filter { it != uid }
                            if (otherContribs.isEmpty()) {
                                ops.add { batch -> batch.delete(doc.reference) }
                            } else {
                                ops.add { batch ->
                                    batch.update(
                                        doc.reference,
                                        mapOf(
                                            "memberIds" to emptyList<String>(),
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                }
                            }
                        } else {
                            ops.add { batch ->
                                batch.update(
                                    doc.reference,
                                    mapOf(
                                        "memberIds" to FieldValue.arrayRemove(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    for (doc in projectsSnap.documents) {
                        if (doc.getString("ownerUid") == uid) {
                            val otherContribs = (doc.get("contributorIds") as? List<*>).orEmpty()
                                .filterIsInstance<String>()
                                .filter { it != uid }
                            if (otherContribs.isEmpty()) {
                                ops.add { batch -> batch.delete(doc.reference) }
                            } else {
                                ops.add { batch ->
                                    batch.update(
                                        doc.reference,
                                        mapOf(
                                            "memberIds" to emptyList<String>(),
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                }
                            }
                        } else {
                            ops.add { batch ->
                                batch.update(
                                    doc.reference,
                                    mapOf(
                                        "memberIds" to FieldValue.arrayRemove(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    for (doc in billsSnap.documents) {
                        if (doc.getString("ownerUid") == uid) {
                            ops.add { batch -> batch.delete(doc.reference) }
                        } else {
                            ops.add { batch ->
                                batch.update(
                                    doc.reference,
                                    mapOf(
                                        "memberIds" to FieldValue.arrayRemove(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    for (doc in categoriesSnap.documents) {
                        if (doc.getString("ownerUid") == uid) {
                            ops.add { batch -> batch.delete(doc.reference) }
                        } else {
                            ops.add { batch ->
                                batch.update(
                                    doc.reference,
                                    mapOf(
                                        "memberIds" to FieldValue.arrayRemove(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }
                        }
                    }

                    ops.add { batch ->
                        batch.update(
                            wsRef,
                            mapOf(
                                "memberIds" to FieldValue.arrayRemove(uid),
                                "members.$uid" to FieldValue.delete(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    commitBatchedOps(ops)
                }
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            Log.w(TAG, "Already removed from workspace $workspaceId; clearing profile reference.")
        }

        clearUserWorkspaceId(uid)
    }

    suspend fun deleteWorkspace(workspace: Workspace) {
        val uid = requireUid()
        require(workspace.ownerUid == uid) { "Only the workspace creator can delete this workspace." }

        val wsRef = workspaceDoc(workspace.id)
        val subcollections = listOf("expenses", "projectContributions", "vehicles", "projects", "bills", "categories")
        val ops = mutableListOf<(WriteBatch) -> Unit>()
        for (sub in subcollections) {
            val snap = wsRef.collection(sub).whereArrayContains("memberIds", uid).get().await()
            for (doc in snap.documents) {
                ops.add { batch -> batch.delete(doc.reference) }
            }
        }
        if (workspace.inviteCode.isNotBlank()) {
            ops.add { batch -> batch.delete(inviteDoc(workspace.inviteCode)) }
        }
        ops.add { batch -> batch.delete(wsRef) }
        commitBatchedOps(ops)
        clearUserWorkspaceId(uid)
    }

    suspend fun clearUserWorkspaceId(uid: String) {
        val currentUid = requireUid()
        require(currentUid == uid)
        val profileRef = userDoc(uid)
        val snap = profileRef.get().await()
        if (snap.exists() && snap.contains("workspaceId")) {
            profileRef.update(
                mapOf(
                    "workspaceId" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun removeMySharedExpensesOnly(workspaceId: String) {
        val uid = requireUid()
        val wsRef = workspaceDoc(workspaceId)
        try {
            val refs = mutableListOf<DocumentReference>()
            for (sub in listOf("expenses", "projectContributions", "vehicles", "projects", "bills", "categories")) {
                val snap = wsRef.collection(sub).whereArrayContains("memberIds", uid).get().await()
                refs += snap.documents.filter { it.getString("ownerUid") == uid }.map { it.reference }
            }
            deleteDocRefs(refs)
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        }
    }

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
    ) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid != uid || uid !in workspace.memberIds) return

        val wsRef = workspaceDoc(workspace.id)
        val memberIds = workspace.memberIds.take(10)
        val cleanOwnerName = ownerName.trim().ifBlank {
            workspace.members[uid]?.name ?: "Member"
        }.take(80)
        val ops = mutableListOf<(WriteBatch) -> Unit>()

        // 1. Sync Categories
        val allActiveSharedCats = existingSharedCategories ?: try {
            wsRef.collection("categories").whereArrayContains("memberIds", uid).get().await()
                .toObjects(SharedCategory::class.java)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return
            throw e
        }
        val currentSharedCats = allActiveSharedCats.filter { it.ownerUid == uid }
        val existingCatsById = currentSharedCats.associateBy { it.id }
        val expectedCatsById = categories.filter { !it.hiddenFromWorkspace && it.id.isNotBlank() }
            .associateBy { "${uid}_${it.id}" }

        existingCatsById.keys.filter { it !in expectedCatsById.keys }.forEach { docId ->
            ops.add { batch -> batch.delete(wsRef.collection("categories").document(docId)) }
        }
        expectedCatsById.forEach { (docId, cat) ->
            val cleanName = cat.name.ifBlank { "Category" }.take(60)
            val cleanIcon = cat.iconKey.ifBlank { "tag" }.take(30)
            val cleanColor = cat.colorIndex.coerceIn(1, 9)
            val cleanSubs = cat.subcategories.take(25)
            val cleanOrder = cat.sortOrder.coerceIn(0, 500)
            val existing = existingCatsById[docId]
            val ref = wsRef.collection("categories").document(docId)
            if (existing == null) {
                ops.add { batch ->
                    batch.set(
                        ref,
                        SharedCategory(
                            id = docId,
                            workspaceId = workspace.id,
                            sourceCategoryId = cat.id,
                            ownerUid = uid,
                            ownerName = cleanOwnerName,
                            memberIds = memberIds,
                            name = cleanName,
                            iconKey = cleanIcon,
                            colorIndex = cleanColor,
                            subcategories = cleanSubs,
                            sortOrder = cleanOrder
                        ).toCreateMap()
                    )
                }
            } else if (existing.ownerName != cleanOwnerName ||
                existing.memberIds != memberIds ||
                existing.name != cleanName ||
                existing.iconKey != cleanIcon ||
                existing.colorIndex != cleanColor ||
                existing.subcategories != cleanSubs ||
                existing.sortOrder != cleanOrder
            ) {
                ops.add { batch ->
                    batch.update(
                        ref,
                        mapOf(
                            "ownerName" to cleanOwnerName,
                            "memberIds" to memberIds,
                            "name" to cleanName,
                            "iconKey" to cleanIcon,
                            "colorIndex" to cleanColor,
                            "subcategories" to cleanSubs,
                            "sortOrder" to cleanOrder,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        // 2. Sync Bills
        val currentSharedBills = existingSharedBills?.filter { it.ownerUid == uid } ?: try {
            wsRef.collection("bills").whereArrayContains("memberIds", uid).get().await()
                .toObjects(SharedBill::class.java).filter { it.ownerUid == uid }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return
            throw e
        }
        val existingBillsById = currentSharedBills.associateBy { it.id }
        val expectedBillsById = bills.filter { !it.hiddenFromWorkspace && it.id.isNotBlank() }
            .associateBy { "${uid}_${it.id}" }

        existingBillsById.keys.filter { it !in expectedBillsById.keys }.forEach { docId ->
            ops.add { batch -> batch.delete(wsRef.collection("bills").document(docId)) }
        }
        expectedBillsById.forEach { (docId, bill) ->
            val cleanRepeat = if (bill.repeat in listOf("Once", "Monthly", "Bi-monthly", "Half-yearly", "Yearly")) bill.repeat else "Monthly"
            val cleanReminder = if (bill.reminder in listOf("On the day", "3 days before", "7 days before")) bill.reminder else "3 days before"
            val existing = existingBillsById[docId]
            val ref = wsRef.collection("bills").document(docId)
            if (existing == null) {
                ops.add { batch ->
                    batch.set(
                        ref,
                        SharedBill(
                            id = docId,
                            workspaceId = workspace.id,
                            sourceBillId = bill.id,
                            ownerUid = uid,
                            ownerName = cleanOwnerName,
                            memberIds = memberIds,
                            name = bill.name,
                            amount = bill.amount,
                            dueEpochMs = bill.dueEpochMs,
                            repeat = cleanRepeat,
                            reminder = cleanReminder,
                            categoryId = bill.categoryId,
                            subcategory = bill.subcategory,
                            iconKey = bill.iconKey,
                            autopay = bill.autopay,
                            paid = bill.paid,
                            paidOnEpochMs = bill.paidOnEpochMs
                        ).toCreateMap()
                    )
                }
            } else if (existing.ownerName != cleanOwnerName ||
                existing.memberIds != memberIds ||
                existing.name != bill.name ||
                existing.amount != bill.amount ||
                existing.dueEpochMs != bill.dueEpochMs ||
                existing.repeat != cleanRepeat ||
                existing.reminder != cleanReminder ||
                existing.categoryId != bill.categoryId ||
                existing.subcategory != bill.subcategory ||
                existing.iconKey != bill.iconKey ||
                existing.autopay != bill.autopay ||
                existing.paid != bill.paid ||
                existing.paidOnEpochMs != bill.paidOnEpochMs
            ) {
                val updates = mutableMapOf<String, Any>(
                    "ownerName" to cleanOwnerName,
                    "memberIds" to memberIds,
                    "name" to bill.name.ifBlank { "Bill" }.take(100),
                    "amount" to bill.amount.coerceAtLeast(1.0),
                    "dueEpochMs" to bill.dueEpochMs.coerceAtLeast(1L),
                    "repeat" to cleanRepeat,
                    "reminder" to cleanReminder,
                    "categoryId" to bill.categoryId.ifBlank { "util" }.take(128),
                    "subcategory" to bill.subcategory.take(80),
                    "iconKey" to bill.iconKey.ifBlank { "bell" }.take(30),
                    "autopay" to bill.autopay,
                    "paid" to bill.paid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updates["paidOnEpochMs"] = bill.paidOnEpochMs?.takeIf { it > 0L } ?: FieldValue.delete()
                ops.add { batch -> batch.update(ref, updates) }
            }
        }

        // 3. Sync Vehicles (preserving contributorIds!)
        val allActiveSharedVehicles = existingSharedVehicles ?: try {
            wsRef.collection("vehicles").whereArrayContains("memberIds", uid).get().await()
                .toObjects(SharedVehicle::class.java)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return
            throw e
        }
        val existingMyVehiclesById = allActiveSharedVehicles.filter { it.ownerUid == uid }.associateBy { it.id }
        val expectedVehiclesById = vehicles.filter { !it.hiddenFromWorkspace && it.id.isNotBlank() }
            .associateBy { "${uid}_${it.id}" }

        existingMyVehiclesById.forEach { (docId, existing) ->
            if (docId !in expectedVehiclesById.keys) {
                val ref = wsRef.collection("vehicles").document(docId)
                val otherContribs = existing.contributorIds.filter { it != uid }
                if (otherContribs.isEmpty()) {
                    ops.add { batch -> batch.delete(ref) }
                } else if (existing.memberIds != listOf(uid)) {
                    ops.add { batch ->
                        batch.update(
                            ref,
                            mapOf(
                                "memberIds" to listOf(uid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
        }
        expectedVehiclesById.forEach { (docId, v) ->
            val existing = existingMyVehiclesById[docId]
            val ref = wsRef.collection("vehicles").document(docId)
            val cleanType = if (v.type == "car") "car" else "bike"
            val cleanPlate = v.plate.ifBlank { "Add plate" }.take(30)
            if (existing == null) {
                ops.add { batch ->
                    batch.set(
                        ref,
                        SharedVehicle(
                            id = docId,
                            workspaceId = workspace.id,
                            sourceVehicleId = v.id,
                            ownerUid = uid,
                            ownerName = cleanOwnerName,
                            memberIds = memberIds,
                            contributorIds = emptyList(),
                            name = v.name,
                            shortName = v.shortName,
                            type = cleanType,
                            plate = cleanPlate,
                            odometer = v.odometer,
                            avgMileage = v.avgMileage,
                            items = v.items,
                            history = v.history
                        ).toCreateMap()
                    )
                }
            } else if (existing.ownerName != cleanOwnerName ||
                existing.memberIds != memberIds ||
                existing.name != v.name ||
                existing.shortName != v.shortName ||
                existing.type != cleanType ||
                existing.plate != cleanPlate ||
                existing.odometer != v.odometer ||
                existing.avgMileage != v.avgMileage ||
                existing.items != v.items ||
                existing.history != v.history
            ) {
                ops.add { batch ->
                    batch.update(
                        ref,
                        mapOf(
                            "ownerName" to cleanOwnerName,
                            "memberIds" to memberIds,
                            "name" to v.name.ifBlank { "Vehicle" }.take(80),
                            "shortName" to v.shortName.ifBlank { "Vehicle" }.take(40),
                            "type" to cleanType,
                            "plate" to cleanPlate,
                            "odometer" to v.odometer.coerceIn(0, 2_000_000),
                            "avgMileage" to v.avgMileage.coerceIn(0.0, 200.0),
                            "items" to v.items.take(40).map { it.toMap() },
                            "history" to v.history.take(100).map { it.toMap() },
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        // 4. Sync Projects (preserving contributorIds!)
        val allActiveSharedProjects = existingSharedProjects ?: try {
            wsRef.collection("projects").whereArrayContains("memberIds", uid).get().await()
                .toObjects(SharedProject::class.java)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return
            throw e
        }
        val existingMyProjectsById = allActiveSharedProjects.filter { it.ownerUid == uid }.associateBy { it.id }
        val expectedProjectsById = projects.filter { !it.hiddenFromWorkspace && it.id.isNotBlank() }
            .associateBy { "${uid}_${it.id}" }

        existingMyProjectsById.forEach { (docId, existing) ->
            if (docId !in expectedProjectsById.keys) {
                val ref = wsRef.collection("projects").document(docId)
                val otherContribs = existing.contributorIds.filter { it != uid }
                if (otherContribs.isEmpty()) {
                    ops.add { batch -> batch.delete(ref) }
                } else if (existing.memberIds != listOf(uid)) {
                    ops.add { batch ->
                        batch.update(
                            ref,
                            mapOf(
                                "memberIds" to listOf(uid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
        }
        expectedProjectsById.forEach { (docId, p) ->
            val existing = existingMyProjectsById[docId]
            val ref = wsRef.collection("projects").document(docId)
            if (existing == null) {
                ops.add { batch ->
                    batch.set(
                        ref,
                        SharedProject(
                            id = docId,
                            workspaceId = workspace.id,
                            sourceProjectId = p.id,
                            ownerUid = uid,
                            ownerName = cleanOwnerName,
                            memberIds = memberIds,
                            contributorIds = emptyList(),
                            name = p.name,
                            subtitle = p.subtitle,
                            iconKey = p.iconKey,
                            colorIndex = p.colorIndex,
                            budget = p.budget,
                            phases = p.phases,
                            vendors = p.vendors,
                            payments = p.payments,
                            productCatalog = p.productCatalog
                        ).toCreateMap()
                    )
                }
            } else if (existing.ownerName != cleanOwnerName ||
                existing.memberIds != memberIds ||
                existing.name != p.name ||
                existing.subtitle != p.subtitle ||
                existing.iconKey != p.iconKey ||
                existing.colorIndex != p.colorIndex ||
                existing.budget != p.budget ||
                existing.phases != p.phases ||
                existing.vendors != p.vendors ||
                existing.payments != p.payments ||
                existing.productCatalog != p.productCatalog
            ) {
                ops.add { batch ->
                    batch.update(
                        ref,
                        mapOf(
                            "ownerName" to cleanOwnerName,
                            "memberIds" to memberIds,
                            "name" to p.name.ifBlank { "Project" }.take(100),
                            "subtitle" to p.subtitle.take(140),
                            "iconKey" to p.iconKey.ifBlank { "house" }.take(30),
                            "colorIndex" to p.colorIndex.coerceIn(1, 9),
                            "budget" to p.budget.coerceAtLeast(1.0),
                            "phases" to p.phases.take(25).map { it.toMap() },
                            "vendors" to p.vendors.take(50).map { it.toMap() },
                            "payments" to p.payments.take(200).map { it.toMap() },
                            "productCatalog" to p.productCatalog.take(60).map { it.toMap() },
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        // 5. Keep my active ProjectContributions memberIds in sync
        val myActiveContribs = existingMyContributions?.filter { it.ownerUid == uid && !it.retainedForDeparture } ?: try {
            wsRef.collection("projectContributions").whereArrayContains("memberIds", uid).get().await()
                .toObjects(SharedProjectContribution::class.java)
                .filter { it.ownerUid == uid && !it.retainedForDeparture }
        } catch (_: FirebaseFirestoreException) {
            emptyList()
        }
        myActiveContribs.forEach { contrib ->
            if (contrib.memberIds != memberIds || contrib.ownerName != cleanOwnerName) {
                ops.add { batch ->
                    batch.update(
                        wsRef.collection("projectContributions").document(contrib.id),
                        mapOf(
                            "memberIds" to memberIds,
                            "ownerName" to cleanOwnerName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        // 6. Sync Shared Expenses (including shared vehicle/project links & fuel/odometer fields)
        val catMap = categories.associateBy { it.id }
        val sharedCatMap = allActiveSharedCats.associateBy { it.sourceCategoryId }
        val hiddenCatIds = categories.filter { it.hiddenFromWorkspace }.map { it.id }.toSet()
        val myVehicleMap = vehicles.associateBy { it.id }
        val otherSharedVehicleMap = allActiveSharedVehicles.filter { it.ownerUid != uid }.associateBy { it.id }
        val myProjectMap = projects.associateBy { it.id }
        val otherSharedProjectMap = allActiveSharedProjects.filter { it.ownerUid != uid }.associateBy { it.id }

        val currentShared = existingSharedByMe?.filter { it.ownerUid == uid && !it.retainedForDeparture } ?: try {
            wsRef.collection("expenses")
                .whereArrayContains("memberIds", uid)
                .get()
                .await()
                .toObjects(SharedExpense::class.java)
                .filter { it.ownerUid == uid && !it.retainedForDeparture }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return
            throw e
        }

        val existingByDocId = currentShared.associateBy { it.id }
        val expectedExpenses = expenses.filter { exp ->
            !exp.isPrivate && exp.categoryId !in hiddenCatIds && exp.id.isNotBlank()
        }
        val expectedByDocId = expectedExpenses.associateBy { "${uid}_${it.id}" }

        existingByDocId.keys
            .filter { it !in expectedByDocId.keys }
            .forEach { docId ->
                val ref = wsRef.collection("expenses").document(docId)
                ops.add { batch -> batch.delete(ref) }
            }

        val foreignVehTargets = mutableListOf<SharedVehicle>()
        val foreignProjTargets = mutableListOf<SharedProject>()

        expectedByDocId.forEach { (docId, exp) ->
            val cat = catMap[exp.categoryId]
            val sharedCat = sharedCatMap[exp.categoryId]
            val catName = (cat?.name ?: sharedCat?.name ?: "Other").take(60)
            val catIcon = (cat?.iconKey ?: sharedCat?.iconKey ?: "tag").take(30)
            val catColor = (cat?.colorIndex ?: sharedCat?.colorIndex ?: 9).coerceIn(1, 9)
            val cleanTitle = exp.title.ifBlank { "Expense" }.take(140)
            val cleanSub = exp.subcategory.take(80)
            val cleanMode = if (exp.paymentMode in listOf("UPI", "Cash", "Card", "Bank")) exp.paymentMode else "UPI"

            val rawVehId = exp.vehicleId?.takeIf { it.isNotBlank() }
            val myVeh = rawVehId?.let { myVehicleMap[it] }
            val otherSharedVeh = rawVehId?.let { otherSharedVehicleMap[it] }
            val resolvedSharedVehId: String? = when {
                myVeh != null && !myVeh.hiddenFromWorkspace -> "${uid}_${myVeh.id}"
                otherSharedVeh != null -> otherSharedVeh.id
                else -> null
            }
            val resolvedSharedVehOwner: String? = when {
                myVeh != null && !myVeh.hiddenFromWorkspace -> uid
                otherSharedVeh != null -> otherSharedVeh.ownerUid
                else -> null
            }
            if (otherSharedVeh != null && uid !in otherSharedVeh.contributorIds) {
                foreignVehTargets += otherSharedVeh
            }

            val rawProjId = exp.projectId?.takeIf { it.isNotBlank() }
            val myProj = rawProjId?.let { myProjectMap[it] }
            val otherSharedProj = rawProjId?.let { otherSharedProjectMap[it] }
            val resolvedSharedProjId: String? = when {
                myProj != null && !myProj.hiddenFromWorkspace -> "${uid}_${myProj.id}"
                otherSharedProj != null -> otherSharedProj.id
                else -> null
            }
            val resolvedSharedProjOwner: String? = when {
                myProj != null && !myProj.hiddenFromWorkspace -> uid
                otherSharedProj != null -> otherSharedProj.ownerUid
                else -> null
            }
            if (otherSharedProj != null && uid !in otherSharedProj.contributorIds) {
                foreignProjTargets += otherSharedProj
            }

            val existing = existingByDocId[docId]
            val ref = wsRef.collection("expenses").document(docId)
            if (existing == null) {
                val payload = SharedExpense(
                    id = docId,
                    workspaceId = workspace.id,
                    sourceExpenseId = exp.id,
                    ownerUid = uid,
                    ownerName = cleanOwnerName,
                    memberIds = memberIds,
                    title = cleanTitle,
                    amount = exp.amount.coerceAtLeast(0.0),
                    dateEpochMs = exp.dateEpochMs.coerceAtLeast(1L),
                    categoryId = exp.categoryId.ifBlank { "groc" }.take(128),
                    categoryName = catName,
                    categoryIconKey = catIcon,
                    categoryColorIndex = catColor,
                    subcategory = cleanSub,
                    paymentMode = cleanMode,
                    sharedVehicleId = resolvedSharedVehId,
                    sharedVehicleOwnerUid = resolvedSharedVehOwner,
                    sharedProjectId = resolvedSharedProjId,
                    sharedProjectOwnerUid = resolvedSharedProjOwner,
                    odometerReading = exp.odometerReading,
                    fuelPricePerLitre = exp.fuelPricePerLitre,
                    fuelLitres = exp.fuelLitres,
                    retainedForDeparture = false
                ).toCreateMap()
                ops.add { batch -> batch.set(ref, payload) }
            } else {
                val needsUpdate = existing.ownerName != cleanOwnerName ||
                    existing.memberIds != memberIds ||
                    existing.title != cleanTitle ||
                    existing.amount != exp.amount ||
                    existing.dateEpochMs != exp.dateEpochMs ||
                    existing.categoryId != exp.categoryId ||
                    existing.categoryName != catName ||
                    existing.categoryIconKey != catIcon ||
                    existing.categoryColorIndex != catColor ||
                    existing.subcategory != cleanSub ||
                    existing.paymentMode != cleanMode ||
                    existing.sharedVehicleId != resolvedSharedVehId ||
                    existing.sharedVehicleOwnerUid != resolvedSharedVehOwner ||
                    existing.sharedProjectId != resolvedSharedProjId ||
                    existing.sharedProjectOwnerUid != resolvedSharedProjOwner ||
                    existing.odometerReading != exp.odometerReading ||
                    existing.fuelPricePerLitre != exp.fuelPricePerLitre ||
                    existing.fuelLitres != exp.fuelLitres
                if (needsUpdate) {
                    val updates = mutableMapOf<String, Any>(
                        "ownerName" to cleanOwnerName,
                        "memberIds" to memberIds,
                        "title" to cleanTitle,
                        "amount" to exp.amount.coerceAtLeast(0.0),
                        "dateEpochMs" to exp.dateEpochMs.coerceAtLeast(1L),
                        "categoryId" to exp.categoryId.ifBlank { "groc" }.take(128),
                        "categoryName" to catName,
                        "categoryIconKey" to catIcon,
                        "categoryColorIndex" to catColor,
                        "subcategory" to cleanSub,
                        "paymentMode" to cleanMode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    updates["sharedVehicleId"] = resolvedSharedVehId ?: FieldValue.delete()
                    updates["sharedVehicleOwnerUid"] = resolvedSharedVehOwner ?: FieldValue.delete()
                    updates["sharedProjectId"] = resolvedSharedProjId ?: FieldValue.delete()
                    updates["sharedProjectOwnerUid"] = resolvedSharedProjOwner ?: FieldValue.delete()
                    updates["odometerReading"] = exp.odometerReading?.takeIf { it >= 0 }?.coerceAtMost(2_000_000) ?: FieldValue.delete()
                    updates["fuelPricePerLitre"] = exp.fuelPricePerLitre?.takeIf { it > 0.0 }?.coerceAtMost(100_000.0) ?: FieldValue.delete()
                    updates["fuelLitres"] = exp.fuelLitres?.takeIf { it > 0.0 }?.coerceAtMost(100_000.0) ?: FieldValue.delete()
                    ops.add { batch -> batch.update(ref, updates) }
                }
            }
        }

        if (ops.isNotEmpty()) {
            commitBatchedOps(ops)
        }

        foreignVehTargets.distinctBy { it.id }.forEach { sv ->
            recordVehicleContributor(workspace.id, sv.id, sv.ownerUid)
        }
        foreignProjTargets.distinctBy { it.id }.forEach { sp ->
            recordProjectContributor(workspace.id, sp.id, sp.ownerUid)
        }
    }

    private suspend fun commitBatchedOps(ops: List<(WriteBatch) -> Unit>) {
        for (chunk in ops.chunked(400)) {
            val batch = db.batch()
            chunk.forEach { it(batch) }
            batch.commit().await()
        }
    }

    private suspend fun deleteDocRefs(refs: List<DocumentReference>) {
        for (chunk in refs.chunked(400)) {
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return buildString(8) {
            repeat(8) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
    }

    private companion object {
        const val TAG = "SelavuWorkspace"
    }
}
