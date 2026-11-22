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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BatchFilter
import com.example.data.model.GymBatch
import com.example.data.model.GymMember
import com.example.data.model.UiState
import com.example.ui.theme.GymAccent
import com.example.ui.theme.GymElevated
import com.example.ui.theme.GymGreen
import com.example.ui.theme.GymInk
import com.example.ui.theme.GymInkMuted
import com.example.ui.theme.GymInkSecondary
import com.example.ui.theme.GymLine
import com.example.ui.theme.GymPaper
import com.example.ui.theme.GymRed
import com.example.ui.theme.GymSurface
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceTab(
    membersState: UiState<List<GymMember>>,
    selectedDate: String,
    selectedBatchFilter: BatchFilter,
    attendanceMap: Map<String, Boolean>,
    allAttendanceDays: Map<String, Map<String, Boolean>>,
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
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = GymInk,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                OutlinedButton(
                    onClick = onExportAttendanceCsv,
                    enabled = allMembers.isNotEmpty(),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("export_attendance_csv_button"),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, GymLine),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymSurface,
                        contentColor = GymInk
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
                        .weight(1.25f)
                        .minimumInteractiveComponentSize()
                        .testTag("att_date_picker_button"),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, GymLine),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymSurface,
                        contentColor = GymInk
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = GymAccent,
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymInk,
                        contentColor = GymPaper
                    ),
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
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GymPaper,
                            letterSpacing = 0.sp
                        ),
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
                    shape = CircleShape,
                    border = BorderStroke(1.dp, GymLine),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymSurface,
                        contentColor = GymInkSecondary
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
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
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
                        shape = CircleShape,
                        color = if (selected) GymInk else GymSurface,
                        border = BorderStroke(1.dp, if (selected) GymInk else GymLine)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (selected) GymPaper else GymInkSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.sp
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
                    accent = GymAccent
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
                        CircularProgressIndicator(color = GymInk)
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
                            shape = RoundedCornerShape(20.dp),
                            color = GymSurface,
                            border = BorderStroke(1.dp, GymLine)
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
                                    tint = GymInkMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (allMembers.isEmpty()) {
                                        "Members tab se pehle members add karo."
                                    } else {
                                        "Is batch filter me koi member nahi hai."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GymInkMuted)
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
                            shape = RoundedCornerShape(18.dp),
                            color = GymSurface,
                            border = BorderStroke(1.dp, GymLine)
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
                                            color = GymInk,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${member.phone} • ${GymBatch.fromId(member.batch).label}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = GymInkSecondary)
                                    )
                                    Text(
                                        text = "Is mahine: $monthCount din aaye",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GymAccent,
                                            letterSpacing = 0.sp
                                        )
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
                                        uncheckedTrackColor = GymElevated,
                                        uncheckedBorderColor = GymLine
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            AttendanceTrendChartCard(
                members = filteredMembers,
                allAttendanceDays = allAttendanceDays,
                anchorDateIso = selectedDate
            )
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
                    Text("OK", color = GymAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = GymInkMuted)
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
        shape = RoundedCornerShape(18.dp),
        color = GymSurface,
        border = BorderStroke(1.dp, GymLine)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = accent,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GymInkMuted,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.sp
                )
            )
        }
    }
}
