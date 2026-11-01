package com.example.util

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val onCodeScanned: (content: String, formatName: String) -> Unit
) : ImageAnalysis.Analyzer {

    // Explicitly configure scanner options to enable 1D linear barcode formats alongside 2D QR codes
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_ITF
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastScannedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastScannedTimestamp < 1200L) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcode = barcodes.firstOrNull { 
                            !it.rawValue.isNullOrBlank() || !it.displayValue.isNullOrBlank()
                        }
                        if (firstBarcode != null) {
                            val value = (firstBarcode.rawValue ?: firstBarcode.displayValue)?.trim()
                            if (!value.isNullOrEmpty() && (System.currentTimeMillis() - lastScannedTimestamp > 1000L)) {
                                lastScannedTimestamp = System.currentTimeMillis()
                                val formatStr = getBarcodeFormatName(firstBarcode.format)
                                onCodeScanned(value, formatStr)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Frame skipped, continue stream
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    companion object {
        // Explicitly enabled barcode formats: 1D linear formats alongside 2D QR codes
        val ENABLED_BARCODE_FORMATS = intArrayOf(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_ITF
        )

        fun createScannerOptions(): BarcodeScannerOptions {
            return BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_ITF
                )
                .build()
        }

        fun scanBitmap(
            bitmap: Bitmap,
            onResult: (content: String?, formatName: String?) -> Unit
        ) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient(createScannerOptions())
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val found = barcodes.firstOrNull { !(it.rawValue ?: it.displayValue).isNullOrBlank() }
                    if (found != null) {
                        val text = (found.rawValue ?: found.displayValue)?.trim()
                        onResult(text, getBarcodeFormatName(found.format))
                    } else {
                        // Fallback to ZXing offline engine
                        val zxingResult = scanBitmapWithZxing(bitmap)
                        if (zxingResult != null) {
                            onResult(zxingResult.first, zxingResult.second)
                        } else {
                            onResult(null, null)
                        }
                    }
                }
                .addOnFailureListener {
                    // Fallback to ZXing offline engine
                    val zxingResult = scanBitmapWithZxing(bitmap)
                    if (zxingResult != null) {
                        onResult(zxingResult.first, zxingResult.second)
                    } else {
                        onResult(null, null)
                    }
                }
        }

        fun scanBitmapWithZxing(bitmap: Bitmap): Pair<String, String>? {
            return try {
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                val hints = mapOf(
                    com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(
                        com.google.zxing.BarcodeFormat.QR_CODE,
                        com.google.zxing.BarcodeFormat.CODE_128,
                        com.google.zxing.BarcodeFormat.CODE_39,
                        com.google.zxing.BarcodeFormat.CODE_93,
                        com.google.zxing.BarcodeFormat.EAN_13,
                        com.google.zxing.BarcodeFormat.EAN_8,
                        com.google.zxing.BarcodeFormat.UPC_A,
                        com.google.zxing.BarcodeFormat.UPC_E,
                        com.google.zxing.BarcodeFormat.ITF,
                        com.google.zxing.BarcodeFormat.CODABAR
                    ),
                    com.google.zxing.DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE
                )
                val reader = com.google.zxing.MultiFormatReader()
                reader.setHints(hints)
                val result = reader.decodeWithState(binaryBitmap)
                val formatReadable = when (result.barcodeFormat) {
                    com.google.zxing.BarcodeFormat.QR_CODE -> "QR Code"
                    com.google.zxing.BarcodeFormat.CODE_128 -> "Code 128 Barcode"
                    com.google.zxing.BarcodeFormat.EAN_13 -> "EAN-13 Product Barcode"
                    com.google.zxing.BarcodeFormat.UPC_A -> "UPC-A Retail Barcode"
                    com.google.zxing.BarcodeFormat.CODE_39 -> "Code 39 Barcode"
                    else -> result.barcodeFormat.name
                }
                Pair(result.text, formatReadable)
            } catch (_: Exception) {
                null
            }
        }

        fun getBarcodeFormatName(format: Int): String {
            return when (format) {
                Barcode.FORMAT_QR_CODE -> "QR Code"
                Barcode.FORMAT_CODE_128 -> "Code 128 Barcode"
                Barcode.FORMAT_CODE_39 -> "Code 39 Barcode"
                Barcode.FORMAT_CODE_93 -> "Code 93 Barcode"
                Barcode.FORMAT_CODABAR -> "Codabar"
                Barcode.FORMAT_DATA_MATRIX -> "Data Matrix 2D"
                Barcode.FORMAT_EAN_13 -> "EAN-13 Product Barcode"
                Barcode.FORMAT_EAN_8 -> "EAN-8 Product Barcode"
                Barcode.FORMAT_ITF -> "ITF Interleaved Barcode"
                Barcode.FORMAT_UPC_A -> "UPC-A Retail Barcode"
                Barcode.FORMAT_UPC_E -> "UPC-E Retail Barcode"
                Barcode.FORMAT_PDF417 -> "PDF417"
                Barcode.FORMAT_AZTEC -> "Aztec"
                else -> "Barcode"
            }
        }
    }
}
