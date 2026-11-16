package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.FeeStatus
import com.example.data.model.GymBatch
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import java.io.File
import java.time.YearMonth

object CsvExportHelper {

    fun buildMembersBackupCsv(members: List<GymMember>): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Phone,Batch,JoinDate")
        members.forEach { member ->
            val batchLabel = GymBatch.fromId(member.batch).label
            sb.appendLine(
                listOf(
                    csvEscape(member.name),
                    csvEscape(member.phone),
                    csvEscape(batchLabel),
                    csvEscape(member.joinDate)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun buildMonthlyFeesCsv(
        month: String,
        members: List<GymMember>,
        feesMap: Map<String, FeeStatus>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Phone,Batch,JoinDate,Month,AmountINR,Status,PaidDate")
        members.forEach { member ->
            val fee = feesMap[member.id]
            val isPaid = fee?.paid == true
            val batchLabel = GymBatch.fromId(member.batch).label
            sb.appendLine(
                listOf(
                    csvEscape(member.name),
                    csvEscape(member.phone),
                    csvEscape(batchLabel),
                    csvEscape(member.joinDate),
                    csvEscape(month),
                    GymConstants.FEES_AMOUNT.toString(),
                    if (isPaid) "Fees Paid" else "Fees Pending",
                    csvEscape(fee?.paidDate.orEmpty())
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun buildMonthlyAttendanceCsv(
        monthPrefix: String,
        members: List<GymMember>,
        allAttendanceDays: Map<String, Map<String, Boolean>>
    ): String {
        val monthDays = allAttendanceDays.keys
            .filter { it.startsWith(monthPrefix) }
            .sorted()
        val totalDaysInMonth = runCatching { YearMonth.parse(monthPrefix).lengthOfMonth() }.getOrDefault(30)

        val sb = StringBuilder()
        val header = mutableListOf("Name", "Phone", "Batch", "Month", "DaysPresent", "TrackedDays")
        header.addAll(monthDays)
        sb.appendLine(header.joinToString(",") { csvEscape(it) })

        members.forEach { member ->
            val batchLabel = GymBatch.fromId(member.batch).label
            val presentCount = monthDays.count { day -> allAttendanceDays[day]?.get(member.id) == true }
            val row = mutableListOf(
                member.name,
                member.phone,
                batchLabel,
                monthPrefix,
                presentCount.toString(),
                "${monthDays.size}/$totalDaysInMonth"
            )
            monthDays.forEach { day ->
                val mark = when (allAttendanceDays[day]?.get(member.id)) {
                    true -> "P"
                    false -> "A"
                    null -> "-"
                }
                row.add(mark)
            }
            sb.appendLine(row.joinToString(",") { csvEscape(it) })
        }
        return sb.toString()
    }

    fun shareCsvFile(
        context: Context,
        fileName: String,
        csvContent: String,
        chooserTitle: String
    ): Result<Unit> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, fileName)
            outFile.writeText(csvContent, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        }
    }

    private fun csvEscape(raw: String): String {
        val needsQuotes = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuotes) return raw
        return "\"${raw.replace("\"", "\"\"")}\""
    }
}
