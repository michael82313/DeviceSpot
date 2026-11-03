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
    fun base32_rfc4648_testVectorsEncodeAndDecode() {
        // RFC 4648 official test vectors
        val testCases = mapOf(
            "" to "",
            "f" to "MY",
            "fo" to "MZXQ",
            "foo" to "MZXW6",
            "foob" to "MZXW6YQ",
            "foobar" to "MZXW6YTBOI"
        )

        for ((input, expected) in testCases) {
            val encoded = MfaSecurity.Base32.encode(input.toByteArray(Charsets.UTF_8))
            assertEquals("Base32 encode failed for '$input'", expected, encoded)

            val decoded = MfaSecurity.Base32.decode(expected)
            assertEquals("Base32 decode failed for '$expected'", input, String(decoded, Charsets.UTF_8))
        }
    }

    @Test
    fun authenticatorSecret_isCompliantRfcBase32() {
        val secret = MfaSecurity.generateSecret()

        // 1. Must be exactly 32 characters for 20-byte HMAC-SHA1 seed
        assertEquals("Secret must be 32 Base32 characters", 32, secret.length)

        // 2. Must only contain RFC 4648 Base32 alphabet: A-Z and 2-7
        assertTrue("Secret must be valid Base32", MfaSecurity.isBase32Secret(secret))
        assertTrue("Secret must only contain A-Z and 2-7", secret.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" })

        // 3. Must not contain invalid characters like 0, 1, 8, 9
        assertFalse("Secret cannot contain '0'", secret.contains("0"))
        assertFalse("Secret cannot contain '1'", secret.contains("1"))
        assertFalse("Secret cannot contain '8'", secret.contains("8"))
        assertFalse("Secret cannot contain '9'", secret.contains("9"))

        // 4. Decodes to exactly 20 bytes
        val decodedBytes = MfaSecurity.Base32.decode(secret)
        assertEquals("Decoded seed must be exactly 20 bytes (160 bits)", 20, decodedBytes.size)
    }

    @Test
    fun authenticatorUri_containsCorrectParameters() {
        val secret = MfaSecurity.generateSecret()
        val uri = MfaSecurity.getOtpAuthUri("user@example.com", secret, "DeviceSpot")

        assertTrue("URI must start with otpauth://totp/", uri.startsWith("otpauth://totp/"))
        assertTrue("URI must contain secret parameter", uri.contains("secret=$secret"))
        assertTrue("URI must specify SHA1 algorithm", uri.contains("algorithm=SHA1"))
        assertTrue("URI must specify 6 digits", uri.contains("digits=6"))
        assertTrue("URI must specify 30s period", uri.contains("period=30"))
        assertTrue("URI must contain issuer", uri.contains("issuer=DeviceSpot"))
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
