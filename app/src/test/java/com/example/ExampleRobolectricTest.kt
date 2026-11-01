package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Device
import com.example.util.QrCodeAnalyzer
import com.example.util.QrCodeGenerator
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DeviceSpot", appName)
    }

    @Test
    fun `test qr code generator generates valid unique code`() {
        val code = QrCodeGenerator.generateUniqueCode()
        assertTrue(code.startsWith("DEV-"))
        assertEquals(8, code.length)
    }

    @Test
    fun `test qr bitmap generation creates bitmap`() {
        val bitmap = QrCodeGenerator.generateQrBitmap("DEV-1082", 200)
        assertNotNull(bitmap)
        assertEquals(200, bitmap?.width)
        assertEquals(200, bitmap?.height)
    }

    @Test
    fun `test 1D barcode generation creates bitmap`() {
        val barcode = QrCodeGenerator.generate1DBarcodeBitmap("DEV-1082", 300, 100)
        assertNotNull(barcode)
        assertTrue(barcode!!.width > 0)
        assertEquals(100, barcode.height)
    }

    @Test
    fun `test device entity default status`() {
        val device = Device(
            qrCode = "DEV-9999",
            name = "Test Sensor"
        )
        assertEquals(Device.STATUS_STORED, device.status)
        assertEquals("Main Storage", device.storageLocation)
    }

    @Test
    fun `test barcode scanner explicitly enables 1D linear formats and QR code`() {
        val formats = QrCodeAnalyzer.ENABLED_BARCODE_FORMATS
        assertTrue("Must support QR Code", formats.contains(Barcode.FORMAT_QR_CODE))
        assertTrue("Must explicitly support Code 128", formats.contains(Barcode.FORMAT_CODE_128))
        assertTrue("Must explicitly support EAN-13", formats.contains(Barcode.FORMAT_EAN_13))
        assertTrue("Must explicitly support UPC-A", formats.contains(Barcode.FORMAT_UPC_A))
        assertTrue("Must support Code 39", formats.contains(Barcode.FORMAT_CODE_39))
    }

    @Test
    fun `test zxing offline scanner decodes generated QR bitmap`() {
        val testPayload = "DEV-9876"
        val bitmap = QrCodeGenerator.generateQrBitmap(testPayload, 200)
        assertNotNull(bitmap)
        val decoded = QrCodeAnalyzer.scanBitmapWithZxing(bitmap!!)
        assertNotNull(decoded)
        assertEquals(testPayload, decoded?.first)
        assertEquals("QR Code", decoded?.second)
    }

    @Test
    fun `test zxing offline scanner decodes generated 1D barcode bitmap`() {
        val testPayload = "DEV-1082"
        val bitmap = QrCodeGenerator.generate1DBarcodeBitmap(testPayload, 300, 100)
        assertNotNull(bitmap)
        val decoded = QrCodeAnalyzer.scanBitmapWithZxing(bitmap!!)
        assertNotNull(decoded)
        assertEquals(testPayload, decoded?.first)
        assertEquals("Code 128 Barcode", decoded?.second)
    }

    @Test
    fun `test barcode format names mapping`() {
        assertEquals("QR Code", QrCodeAnalyzer.getBarcodeFormatName(Barcode.FORMAT_QR_CODE))
        assertEquals("Code 128 Barcode", QrCodeAnalyzer.getBarcodeFormatName(Barcode.FORMAT_CODE_128))
        assertEquals("UPC-A Retail Barcode", QrCodeAnalyzer.getBarcodeFormatName(Barcode.FORMAT_UPC_A))
        assertEquals("EAN-13 Product Barcode", QrCodeAnalyzer.getBarcodeFormatName(Barcode.FORMAT_EAN_13))
    }
}
