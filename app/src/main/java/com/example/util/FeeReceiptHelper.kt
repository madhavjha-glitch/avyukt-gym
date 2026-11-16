package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.GymBatch
import com.example.data.model.GymConstants
import com.example.data.model.GymMember
import com.example.ui.admin.AdminViewModel
import java.io.File
import java.io.FileOutputStream

object FeeReceiptHelper {

    fun generateAndShareReceiptPdf(
        context: Context,
        member: GymMember,
        month: String,
        paidDate: String?,
        chooserTitle: String
    ): Result<Unit> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeMonth = month.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val safeId = member.id.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "member" }
            val outFile = File(exportDir, "avyukt_gym_receipt_${safeId}_${safeMonth}.pdf")

            val pageWidth = 420
            val pageHeight = 595
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)

            drawReceiptPage(
                canvas = page.canvas,
                context = context,
                member = member,
                month = month,
                paidDate = paidDate,
                width = pageWidth.toFloat(),
                height = pageHeight.toFloat()
            )

            document.finishPage(page)
            FileOutputStream(outFile).use { stream ->
                document.writeTo(stream)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Avyukt Gym Fees Receipt - ${member.name} (${AdminViewModel.formatMonthLabel(month)})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Namaste ${member.name} ji, Avyukt Gym ki ${AdminViewModel.formatMonthLabel(month)} mahine ki ₹${GymConstants.FEES_AMOUNT} fees receipt attach hai. Dhanyavaad!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        }
    }

    private fun drawReceiptPage(
        canvas: Canvas,
        context: Context,
        member: GymMember,
        month: String,
        paidDate: String?,
        width: Float,
        height: Float
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#FFFFFF") }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Header Banner
        val headerPaint = Paint().apply { color = Color.parseColor("#111318") }
        canvas.drawRect(0f, 0f, width, 126f, headerPaint)

        val accentBarPaint = Paint().apply { color = Color.parseColor("#FF6B00") }
        canvas.drawRect(0f, 122f, width, 128f, accentBarPaint)

        // Gym Logo (optional graceful decode)
        val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.gym_logo)
        if (rawLogo != null) {
            val logoSize = 56
            val scaled = Bitmap.createScaledBitmap(rawLogo, logoSize, logoSize, true)
            val clipPath = android.graphics.Path().apply {
                addCircle(48f, 62f, 28f, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(scaled, 20f, 34f, null)
            canvas.restore()

            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = Color.parseColor("#FF6B00")
            }
            canvas.drawCircle(48f, 62f, 28f, ringPaint)
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AVYUKT GYM", 92f, 56f, brandPaint)

        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB703")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("OFFICIAL FEES RECEIPT", 92f, 75f, subBrandPaint)

        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0B7C3")
            textSize = 10.5f
        }
        canvas.drawText("Call / WhatsApp: ${GymConstants.OWNER_PHONE_DISPLAY}", 92f, 94f, contactPaint)

        // Receipt Meta Row
        val receiptNo = "AVG-${month.replace("-", "")}-${member.id.take(4).uppercase().ifBlank { "0001" }}"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 11f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Receipt No:", 24f, 160f, labelPaint)
        canvas.drawText(receiptNo, 95f, 160f, valuePaint)

        // Paid Badge Pill on top-right of body
        val badgeRect = RectF(width - 118f, 142f, width - 24f, 168f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DCFCE7") }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#16A34A")
        }
        canvas.drawRoundRect(badgeRect, 13f, 13f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 13f, 13f, badgeBorderPaint)
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#15803D")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FEES PAID", badgeRect.centerX(), badgeRect.centerY() + 4f, badgeTextPaint)

        // Amount Highlight Box
        val amountCard = RectF(24f, 186f, width - 24f, 260f)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF7ED") }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#FDBA74")
        }
        canvas.drawRoundRect(amountCard, 12f, 12f, cardBgPaint)
        canvas.drawRoundRect(amountCard, 12f, 12f, cardBorderPaint)

        val amountTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9A3412")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AMOUNT RECEIVED", 40f, 212f, amountTitlePaint)

        val amountBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("₹${GymConstants.FEES_AMOUNT}", 40f, 245f, amountBigPaint)

        val monthPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(AdminViewModel.formatMonthLabel(month), width - 40f, 232f, monthPillPaint)

        // Details Table
        val batch = GymBatch.fromId(member.batch)
        val rows = listOf(
            "Member Name" to member.name,
            "Phone Number" to member.phone,
            "Batch" to "${batch.label} (${batch.timingHint})",
            "Joining Date" to AdminViewModel.formatDisplayDate(member.joinDate),
            "Fee Month" to AdminViewModel.formatMonthLabel(month),
            "Payment Date" to if (!paidDate.isNullOrBlank()) AdminViewModel.formatDisplayDate(paidDate) else AdminViewModel.formatDisplayDate(AdminViewModel.todayIso()),
            "Plan" to "Monthly Membership (₹${GymConstants.FEES_AMOUNT}/mo)"
        )

        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }
        var y = 296f
        rows.forEach { (key, value) ->
            canvas.drawText(key, 28f, y, labelPaint)
            val valRightPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(value, width - 28f, y, valRightPaint)
            y += 14f
            canvas.drawLine(24f, y, width - 24f, y, linePaint)
            y += 22f
        }

        // Footer
        val footerBox = RectF(24f, height - 86f, width - 24f, height - 24f)
        val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F9FAFB") }
        canvas.drawRoundRect(footerBox, 10f, 10f, footerBg)

        val footerBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val footerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for training with Avyukt Gym!", width / 2f, height - 58f, footerBold)
        canvas.drawText("Mon–Sat · Morning 5:00–10:00 AM · Evening 5:00–10:00 PM", width / 2f, height - 40f, footerSub)
    }
}
