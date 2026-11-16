package com.example.ui.admin

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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BatchFilter
import com.example.data.model.GymBatch
import com.example.data.model.GymMember
import com.example.data.model.UiState
import com.example.ui.theme.GymBorder
import com.example.ui.theme.GymCardBg
import com.example.ui.theme.GymElevated
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymOrange
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymTextMuted
import com.example.ui.theme.GymTextPrimary
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceTab(
    membersState: UiState<List<GymMember>>,
    selectedDate: String,
    selectedBatchFilter: BatchFilter,
    attendanceMap: Map<String, Boolean>,
    monthlyCounts: Map<String, Int>,
    onDateChange: (String) -> Unit,
    onBatchFilterChange: (BatchFilter) -> Unit,
    onToggleMember: (String) -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onOpenMemberHistory: (GymMember) -> Unit,
    onExportAttendanceCsv: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val allMembers = (membersState as? UiState.Success)?.data ?: emptyList()
    val filteredMembers = remember(allMembers, selectedBatchFilter) {
        allMembers.filter { GymBatch.fromId(it.batch).matchesFilter(selectedBatchFilter) }
    }
    val presentCount = filteredMembers.count { attendanceMap[it.id] == true }
    val absentCount = (filteredMembers.size - presentCount).coerceAtLeast(0)

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
                        imageVector = Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        tint = GymOrange
                    )
                    Text(
                        text = "ATTENDANCE",
                        style = MaterialTheme.typography.headlineMedium.copy(color = GymTextPrimary)
                    )
                }

                OutlinedButton(
                    onClick = onExportAttendanceCsv,
                    enabled = allMembers.isNotEmpty(),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("export_attendance_csv_button"),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GymOrange.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymOrange.copy(alpha = 0.12f),
                        contentColor = GymOrange
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.export_attendance_csv),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1.3f)
                        .minimumInteractiveComponentSize()
                        .testTag("att_date_picker_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GymBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymElevated,
                        contentColor = GymTextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = GymOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AdminViewModel.formatDisplayDate(selectedDate),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = { onMarkAll(true) },
                    enabled = filteredMembers.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("mark_all_present_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = GymOrange),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.all_present),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { onMarkAll(false) },
                    enabled = filteredMembers.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("mark_all_absent_button"),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GymBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymElevated,
                        contentColor = GymTextMuted
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.all_absent),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
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
                            .testTag("att_batch_filter_${filter.name.lowercase()}"),
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AttCountCard(
                    modifier = Modifier.weight(1f),
                    count = presentCount,
                    label = stringResource(R.string.present_label),
                    accent = GymGreen
                )
                AttCountCard(
                    modifier = Modifier.weight(1f),
                    count = absentCount,
                    label = stringResource(R.string.absent_label),
                    accent = GymRed
                )
                AttCountCard(
                    modifier = Modifier.weight(1f),
                    count = filteredMembers.size,
                    label = stringResource(R.string.total_label),
                    accent = GymOrange
                )
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
                                    imageVector = Icons.Rounded.EventAvailable,
                                    contentDescription = null,
                                    tint = GymTextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (allMembers.isEmpty()) {
                                        "Members tab se pehle members add karo."
                                    } else {
                                        "Is batch filter me koi member nahi hai."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMembers, key = { it.id }) { member ->
                        val isPresent = attendanceMap[member.id] == true
                        val monthCount = monthlyCounts[member.id] ?: 0
                        Surface(
                            onClick = { onOpenMemberHistory(member) },
                            modifier = Modifier.fillMaxWidth(),
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
                                        text = "${member.phone} • ${GymBatch.fromId(member.batch).label}",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GymTextMuted)
                                    )
                                    Text(
                                        text = "Is mahine: $monthCount din aaye",
                                        style = MaterialTheme.typography.labelSmall.copy(color = GymOrange)
                                    )
                                }

                                Switch(
                                    checked = isPresent,
                                    onCheckedChange = { onToggleMember(member.id) },
                                    modifier = Modifier.testTag("att_switch_${member.id}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GymGreen,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
                            onDateChange(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = GymOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = GymTextMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AttCountCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = accent)
            )
        }
    }
}
