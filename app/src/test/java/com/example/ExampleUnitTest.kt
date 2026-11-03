package com.example

import com.example.util.MfaSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun backupCodes_generation_producesEightUniqueTenDigitCodes() {
        val codes = MfaSecurity.generateBackupCodes(8)

        // 1. Must produce exactly 8 codes
        assertEquals("Must generate exactly 8 backup codes", 8, codes.size)

        // 2. All codes must be unique
        val uniqueCodes = codes.toSet()
        assertEquals("All 8 codes must be unique and non-overlapping", 8, uniqueCodes.size)

        // 3. Each code must be strictly 10 numeric digits
        codes.forEachIndexed { index, code ->
            assertEquals("Code at index $index must be 10 characters long", 10, code.length)
            assertTrue("Code at index $index must contain only numeric digits: $code", code.all { it.isDigit() })
            assertFalse("Code at index $index cannot be blank", code.isBlank())
        }
    }

    @Test
    fun backupCodes_formatting_splitsIntoFiveDigitsChunks() {
        val formatted = MfaSecurity.formatBackupCode("1234567890")
        assertEquals("12345 67890", formatted)
    }

    @Test
    fun backupCodes_supportTicketUrl_matchesExpectedFormUrl() {
        val expected = "https://docs.google.com/forms/d/e/1FAIpQLSfXMACZYpNXz10nZCllbkmcKkWJtlWA_RdNYstyPZS_ir7wzw/viewform?usp=publish-editor"
        assertEquals(expected, MfaSecurity.SUPPORT_TICKET_URL)
    }

    @Test
    fun backupCodes_exportText_containsCodesAndSupportUrl() {
        val codes = MfaSecurity.generateBackupCodes(8)
        val text = MfaSecurity.buildBackupCodesText("test@example.com", codes)

        assertTrue("Export text must mention account email", text.contains("test@example.com"))
        assertTrue("Export text must mention support ticket url", text.contains(MfaSecurity.SUPPORT_TICKET_URL))
        codes.forEach { code ->
            assertTrue("Export text must include formatted code $code", text.contains(MfaSecurity.formatBackupCode(code)))
        }
    }
}
