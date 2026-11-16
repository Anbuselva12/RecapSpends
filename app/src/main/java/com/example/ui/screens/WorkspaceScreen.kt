package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ServiceStatus
import com.example.data.model.Workspace
import com.example.data.model.WorkspaceMember
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuTextField
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.SelavuTheme

@Composable
fun WorkspaceScreen(
    currentUid: String,
    workspace: Workspace?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onCreateWorkspace: (String) -> Unit,
    onJoinWorkspace: (String) -> Unit,
    onRegenerateCode: (Workspace) -> Unit,
    onRemoveMember: (Workspace, String, String) -> Unit,
    onLeaveWorkspace: (String) -> Unit,
    onDeleteWorkspace: (Workspace) -> Unit,
    onShowToast: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var newWorkspaceName by rememberSaveable { mutableStateOf("") }
    var joinCodeInput by rememberSaveable { mutableStateOf("") }

    var memberToKick by remember { mutableStateOf<WorkspaceMember?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    memberToKick?.let { member ->
        if (workspace != null) {
            AlertDialog(
                onDismissRequest = { memberToKick = null },
                containerColor = colorScheme.surface,
                titleContentColor = colorScheme.onSurface,
                textContentColor = colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.extraLarge,
                title = {
                    Text(
                        text = "Remove ${member.name}?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "All expenses shared by ${member.name} will be permanently removed from this workspace and sharing stops immediately. Their personal expenses stay untouched on their own account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onRemoveMember(workspace, member.uid, member.name)
                            memberToKick = null
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.error,
                            contentColor = colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_kick_member")
                    ) {
                        Text("Remove member", style = MaterialTheme.typography.labelLarge)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { memberToKick = null }) {
                        Text("Cancel", color = colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }

    if (showLeaveConfirm && workspace != null) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = "Leave ${workspace.name}?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "All of your shared expenses will be completely removed from this workspace and you will stop seeing others' shared expenses. Your personal entries remain safe.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        onLeaveWorkspace(workspace.id)
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_leave_workspace")
                ) {
                    Text("Leave workspace", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteConfirm && workspace != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = "Delete ${workspace.name}?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This permanently dissolves the shared workspace and removes all shared copies for every member. Each member's personal expenses remain untouched.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteWorkspace(workspace)
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_workspace")
                ) {
                    Text("Delete workspace", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelavuTopNavBar(title = "Family Workspace", onBack = onBack)
            }

            if (workspace == null) {
                // Create or Join Workspace
                item {
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Groups,
                                        contentDescription = "Family Workspace",
                                        tint = colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Share household spends selectively",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Keep private entries or entire categories hidden while viewing shared family spends together.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Create a new workspace", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                            SelavuTextField(
                                value = newWorkspaceName,
                                onValueChange = { newWorkspaceName = it },
                                label = "Workspace name",
                                placeholder = "e.g. Kumar Family, Home Budget",
                                modifier = Modifier.fillMaxWidth().testTag("workspace_name_input")
                            )
                            Button(
                                onClick = {
                                    if (newWorkspaceName.isBlank()) {
                                        onShowToast("Enter a workspace name")
                                    } else {
                                        onCreateWorkspace(newWorkspaceName.trim())
                                    }
                                },
                                enabled = !isSaving,
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("create_workspace_button")
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create workspace", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Join with an invite code", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                            SelavuTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase() },
                                label = "Invite code",
                                placeholder = "e.g. FAM7K9P2",
                                modifier = Modifier.fillMaxWidth().testTag("join_workspace_code_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    if (joinCodeInput.trim().length < 6) {
                                        onShowToast("Enter a valid invite code")
                                    } else {
                                        onJoinWorkspace(joinCodeInput.trim())
                                    }
                                },
                                enabled = !isSaving,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("join_workspace_button")
                            ) {
                                Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Join workspace", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            } else {
                val isOwner = workspace.ownerUid == currentUid

                // Workspace Summary & Invite Code Card
                item {
                    SelavuCard(modifier = Modifier.testTag("active_workspace_card")) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = workspace.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${workspace.memberIds.size}/10 members active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = if (isOwner) "Creator" else "Member",
                                    isAccent = isOwner,
                                    status = if (!isOwner) ServiceStatus.OK else null
                                )
                            }

                            SelavuDivider()

                            Text(
                                text = "FAMILY INVITE CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.outline
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colorScheme.surfaceVariant)
                                    .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = workspace.inviteCode,
                                    style = MonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(workspace.inviteCode))
                                            onShowToast("Invite code copied")
                                        },
                                        modifier = Modifier.size(48.dp).testTag("copy_workspace_code")
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy invite code")
                                    }
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Join our RecapSpends family workspace \"${workspace.name}\" using invite code: ${workspace.inviteCode}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share invite code"))
                                        },
                                        modifier = Modifier.size(48.dp).testTag("share_workspace_code")
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "Share invite code")
                                    }
                                    if (isOwner) {
                                        IconButton(
                                            onClick = { onRegenerateCode(workspace) },
                                            enabled = !isSaving,
                                            modifier = Modifier.size(48.dp).testTag("refresh_workspace_code")
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = "Regenerate invite code")
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = extendedColors.ok,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = "Private expenses and categories marked 'Hide from Family Workspace' stay visible only to you.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Members List
                item {
                    val orderedMembers = remember(workspace.members, workspace.ownerUid) {
                        workspace.members.values.sortedWith(
                            compareBy<WorkspaceMember> { if (it.uid == workspace.ownerUid) 0 else 1 }
                                .thenBy { it.name.lowercase() }
                        )
                    }
                    SectionHeader(title = "Members (${orderedMembers.size})")
                    SelavuListContainer {
                        orderedMembers.forEachIndexed { idx, member ->
                            if (idx > 0) SelavuDivider()
                            val isSelf = member.uid == currentUid
                            val isCreator = member.uid == workspace.ownerUid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "M",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSelf) "${member.name} (You)" else member.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isCreator) "Workspace creator" else "Family member",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isOwner && !isCreator) {
                                    OutlinedButton(
                                        onClick = { memberToKick = member },
                                        enabled = !isSaving,
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(42.dp)
                                            .testTag("kick_member_${member.uid}")
                                    ) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Leave / Delete Action
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isOwner) {
                        Button(
                            onClick = { showDeleteConfirm = true },
                            enabled = !isSaving,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.error,
                                contentColor = colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("delete_workspace_button")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete workspace for everyone", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showLeaveConfirm = true },
                            enabled = !isSaving,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("leave_workspace_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leave workspace & remove my shares", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
