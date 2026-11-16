package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GymBatch
import com.example.data.model.GymMember
import com.example.ui.theme.GymBorder
import com.example.ui.theme.GymDarkSurface
import com.example.ui.theme.GymElevated
import com.example.ui.theme.GymOrange
import com.example.ui.theme.GymTextMuted
import com.example.ui.theme.GymTextPrimary
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberFormDialog(
    existingMember: GymMember?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, joinDate: String, batch: GymBatch) -> Unit
) {
    val isEdit = existingMember != null
    var name by remember(existingMember) { mutableStateOf(existingMember?.name.orEmpty()) }
    var phone by remember(existingMember) { mutableStateOf(existingMember?.phone.orEmpty()) }
    var joinDate by remember(existingMember) {
        mutableStateOf(existingMember?.joinDate?.ifBlank { AdminViewModel.todayIso() } ?: AdminViewModel.todayIso())
    }
    var selectedBatch by remember(existingMember) {
        mutableStateOf(GymBatch.fromId(existingMember?.batch))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = GymDarkSurface,
        title = {
            Text(
                text = stringResource(if (isEdit) R.string.edit_member_title else R.string.add_member_title).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(color = GymTextPrimary)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_name_input"),
                    label = { Text(stringResource(R.string.member_name_label)) },
                    placeholder = { Text("Jaise: Rahul Sharma") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymOrange,
                        unfocusedBorderColor = GymBorder,
                        focusedTextColor = GymTextPrimary,
                        unfocusedTextColor = GymTextPrimary,
                        focusedLabelColor = GymOrange,
                        unfocusedLabelColor = GymTextMuted
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        phone = input.filter { it.isDigit() }.take(10)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_member_phone_input"),
                    label = { Text(stringResource(R.string.member_phone_label)) },
                    placeholder = { Text("10 digit number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymOrange,
                        unfocusedBorderColor = GymBorder,
                        focusedTextColor = GymTextPrimary,
                        unfocusedTextColor = GymTextPrimary,
                        focusedLabelColor = GymOrange,
                        unfocusedLabelColor = GymTextMuted
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.member_batch_label),
                        style = MaterialTheme.typography.labelMedium.copy(color = GymTextMuted)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GymBatch.entries.forEach { batch ->
                            val selected = selectedBatch == batch
                            Surface(
                                onClick = { selectedBatch = batch },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("batch_option_${batch.id.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) GymOrange.copy(alpha = 0.2f) else GymElevated,
                                border = BorderStroke(1.dp, if (selected) GymOrange else GymBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = batch.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (selected) GymOrange else GymTextPrimary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GymBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GymElevated,
                        contentColor = GymTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = GymOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.member_join_date_label)}: ${AdminViewModel.formatDisplayDate(joinDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, phone, joinDate, selectedBatch) },
                enabled = !isSaving && name.isNotBlank() && phone.length >= 10,
                modifier = Modifier.testTag("submit_add_member_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GymOrange)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        stringResource(
                            if (isEdit) R.string.save_changes_button else R.string.submit_add_member
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel", color = GymTextMuted)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            joinDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
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
