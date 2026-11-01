package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.Toast
import androidx.print.PrintHelper

object PrintTagHelper {

    /**
     * Builds and prints a professional, ready-to-stick asset tag with either
     * a QR code or 1D barcode, title, human-readable ID, and home storage location.
     */
    fun printAssetTag(
        context: Context,
        deviceTitle: String,
        code: String,
        location: String = "",
        isBarcode: Boolean = false
    ) {
        if (code.isBlank()) {
            Toast.makeText(context, "Cannot print empty code", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val labelBitmap = createPrintableLabelBitmap(
                title = deviceTitle.ifBlank { "Asset Tag" },
                code = code,
                location = location,
                isBarcode = isBarcode
            )

            if (labelBitmap == null) {
                Toast.makeText(context, "Failed to render tag bitmap for printing", Toast.LENGTH_SHORT).show()
                return
            }

            val printHelper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }

            val jobName = "DeviceSpot_Tag_${code.replace("[^A-Za-z0-9_-]".toRegex(), "_")}"
            printHelper.printBitmap(jobName, labelBitmap)
            Toast.makeText(context, "Opening printer preview for $code...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Printing error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Renders a crisp 800x560 printable sticker bitmap with header, code graphic, and labels.
     */
    fun createPrintableLabelBitmap(
        title: String,
        code: String,
        location: String,
        isBarcode: Boolean
    ): Bitmap? {
        val width = 900
        val height = if (isBarcode) 600 else 760
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill white background
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 205, 215)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val cardRect = RectF(16f, 16f, width - 16f, height - 16f)
        canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

        // Header band
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Dark Slate
            style = Paint.Style.FILL
        }
        val headerRect = RectF(16f, 16f, width - 16f, 96f)
        canvas.drawRoundRect(headerRect, 24f, 24f, headerPaint)
        // Rectify bottom corners of header
        canvas.drawRect(RectF(16f, 60f, width - 16f, 96f), headerPaint)

        // Header text
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DEVICESPOT HARDWARE ASSET TAG", width / 2f, 64f, headerTextPaint)

        // Device Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val displayTitle = if (title.length > 35) title.take(32) + "..." else title
        canvas.drawText(displayTitle, width / 2f, 150f, titlePaint)

        if (isBarcode) {
            // Draw 1D Barcode
            val barcodeBm = QrCodeGenerator.generate1DBarcodeBitmap(
                content = code,
                width = 720,
                height = 220
            )
            if (barcodeBm != null) {
                val left = (width - barcodeBm.width) / 2f
                canvas.drawBitmap(barcodeBm, left, 185f, null)
            }
        } else {
            // Draw 2D QR Code
            val qrBm = QrCodeGenerator.generateQrBitmap(
                content = code,
                size = 380
            )
            if (qrBm != null) {
                val left = (width - qrBm.width) / 2f
                canvas.drawBitmap(qrBm, left, 180f, null)
            }
        }

        // Code identifier (human readable)
        val codeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val codeY = if (isBarcode) 450f else 610f
        canvas.drawText("* $code *", width / 2f, codeY, codeTextPaint)

        // Location / Subtitle
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val locText = if (location.isNotBlank()) "Storage: $location" else "Scan with DeviceSpot or any standard camera"
        val locY = if (isBarcode) 500f else 665f
        canvas.drawText(locText, width / 2f, locY, subPaint)

        return bitmap
    }
}
