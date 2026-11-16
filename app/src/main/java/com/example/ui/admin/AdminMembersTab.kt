package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BatchFilter
import com.example.data.model.DueUrgency
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymMember
import com.example.data.model.UiState
import com.example.data.model.computeMemberDueInfo
import com.example.ui.theme.GymBlue
import com.example.ui.theme.GymBorder
import com.example.ui.theme.GymCardBg
import com.example.ui.theme.GymDarkSurface
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymOrange
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymTextMuted
import com.example.ui.theme.GymTextPrimary

@Composable
fun AdminMembersTab(
    membersState: UiState<List<GymMember>>,
    currentMonthFees: Map<String, FeeStatus>,
    searchQuery: String,
    selectedBatchFilter: BatchFilter,
    isSavingMember: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onBatchFilterChange: (BatchFilter) -> Unit,
    onAddMember: (name: String, phone: String, joinDate: String, batch: GymBatch, onSuccess: () -> Unit) -> Unit,
    onUpdateMember: (memberId: String, name: String, phone: String, joinDate: String, batch: GymBatch, onSuccess: () -> Unit) -> Unit,
    onDeleteMember: (GymMember) -> Unit,
    onOpenMemberHistory: (GymMember) -> Unit,
    onExportMembersCsv: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var memberBeingEdited by remember { mutableStateOf<GymMember?>(null) }
    var memberPendingDelete by remember { mutableStateOf<GymMember?>(null) }

    val allMembers = (membersState as? UiState.Success)?.data ?: emptyList()
    val filteredMembers = remember(allMembers, searchQuery, selectedBatchFilter) {
        allMembers.filter { member ->
            val matchesBatch = GymBatch.fromId(member.batch).matchesFilter(selectedBatchFilter)
            val matchesQuery = searchQuery.isBlank() ||
                member.name.contains(searchQuery, ignoreCase = true) ||
                member.phone.contains(searchQuery)
            matchesBatch && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = GymOrange
                    )
                    Text(
                        text = "MEMBERS",
                        style = MaterialTheme.typography.headlineMedium.copy(color = GymTextPrimary)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GymOrange
                    ) {
                        Text(
                            text = allMembers.size.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExportMembersCsv,
                        enabled = allMembers.isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("export_members_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.export_members_csv),
                            tint = GymOrange
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_member_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GymOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(50),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_members_hint),
                        color = GymTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = GymTextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GymOrange,
                    unfocusedBorderColor = GymBorder,
                    focusedContainerColor = GymCardBg,
                    unfocusedContainerColor = GymCardBg,
                    focusedTextColor = GymTextPrimary,
                    unfocusedTextColor = GymTextPrimary
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatchFilter.entries.forEach { filter ->
                    val count = allMembers.count { GymBatch.fromId(it.batch).matchesFilter(filter) }
                    val selected = selectedBatchFilter == filter
                    Surface(
                        onClick = { onBatchFilterChange(filter) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("batch_filter_${filter.name.lowercase()}"),
                        shape = RoundedCornerShape(50),
                        color = if (selected) GymOrange.copy(alpha = 0.2f) else GymCardBg,
                        border = BorderStroke(1.dp, if (selected) GymOrange else GymBorder)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (selected) GymOrange else GymTextMuted,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        when (membersState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GymOrange)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = membersState.message,
                        color = GymRed,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is UiState.Success -> {
                if (filteredMembers.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = GymCardBg,
                            border = BorderStroke(1.dp, GymBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = GymTextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank() || selectedBatchFilter != BatchFilter.ALL) {
                                        "Is filter me koi member nahi mila."
                                    } else {
                                        "Koi member nahi hai. Add button se add karo."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMembers, key = { it.id }) { member ->
                        val batch = GymBatch.fromId(member.batch)
                        val isPaid = currentMonthFees[member.id]?.paid == true
                        val dueInfo = remember(member.joinDate, isPaid) {
                            computeMemberDueInfo(member.joinDate, isPaid)
                        }
                        val (dueLabel, dueColor) = when (dueInfo.urgency) {
                            DueUrgency.PAID -> "Paid" to GymGreen
                            DueUrgency.OVERDUE -> "Overdue ${-dueInfo.daysRemaining}d" to GymRed
                            DueUrgency.DUE_SOON -> if (dueInfo.daysRemaining == 0L) "Due Today" to GymOrange else "Due ${dueInfo.daysRemaining}d" to GymOrange
                            DueUrgency.UPCOMING -> "Pending" to GymTextMuted
                        }

                        Surface(
                            onClick = { onOpenMemberHistory(member) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_card_${member.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = GymCardBg,
                            border = BorderStroke(1.dp, GymBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemberAvatarBadge(memberId = member.id, name = member.name)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = GymTextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = member.phone,
                                        style = MaterialTheme.typography.bodySmall.copy(color = GymTextMuted)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = GymBlue.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = batch.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = GymBlue,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = dueColor.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = dueLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = dueColor,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { memberBeingEdited = member },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_member_${member.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit ${member.name}",
                                        tint = GymOrange
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone}"))
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = "Call ${member.name}",
                                        tint = GymGreen
                                    )
                                }

                                IconButton(
                                    onClick = { memberPendingDelete = member },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete ${member.name}",
                                        tint = GymRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showAddDialog) {
        MemberFormDialog(
            existingMember = null,
            isSaving = isSavingMember,
            onDismiss = { showAddDialog = false },
            onSubmit = { name, phone, joinDate, batch ->
                onAddMember(name, phone, joinDate, batch) {
                    showAddDialog = false
                }
            }
        )
    }

    val editTarget = memberBeingEdited
    if (editTarget != null) {
        MemberFormDialog(
            existingMember = editTarget,
            isSaving = isSavingMember,
            onDismiss = { memberBeingEdited = null },
            onSubmit = { name, phone, joinDate, batch ->
                onUpdateMember(editTarget.id, name, phone, joinDate, batch) {
                    memberBeingEdited = null
                }
            }
        )
    }

    val toDelete = memberPendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { memberPendingDelete = null },
            containerColor = GymDarkSurface,
            title = {
                Text(
                    text = "${toDelete.name} ko delete karo?",
                    color = GymTextPrimary
                )
            },
            text = {
                Text(
                    text = "Is member ko delete karne par list se hata diya jayega.",
                    color = GymTextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(toDelete)
                        memberPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GymRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingDelete = null }) {
                    Text("Cancel", color = GymTextMuted)
                }
            }
        )
    }
}

