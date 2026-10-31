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
                        val firstBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                        if (firstBarcode != null) {
                            val value = firstBarcode.rawValue?.trim()
                            if (!value.isNullOrEmpty() && (System.currentTimeMillis() - lastScannedTimestamp > 1200L)) {
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
                    val found = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (found != null) {
                        onResult(found.rawValue?.trim(), getBarcodeFormatName(found.format))
                    } else {
                        onResult(null, null)
                    }
                }
                .addOnFailureListener {
                    onResult(null, null)
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
