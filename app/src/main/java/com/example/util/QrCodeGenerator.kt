package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Generates a clean Bitmap QR code for a given text payload.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1) // compact margin
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a linear 1D Barcode (Code 128) Bitmap for asset stickers.
     */
    fun generate1DBarcodeBitmap(
        content: String,
        width: Int = 600,
        height: Int = 200,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 2)
            }

            val bitMatrix = Code128Writer().encode(
                content,
                BarcodeFormat.CODE_128,
                width,
                height,
                hints
            )

            val bmWidth = bitMatrix.width
            val bmHeight = bitMatrix.height
            val pixels = IntArray(bmWidth * bmHeight)

            for (y in 0 until bmHeight) {
                val offset = y * bmWidth
                for (x in 0 until bmWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, bmWidth, 0, 0, bmWidth, bmHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a short, clean, unique QR/Barcode ID like "DEV-8491"
     */
    fun generateUniqueCode(): String {
        val rand = (1000..9999).random()
        return "DEV-$rand"
    }
}
