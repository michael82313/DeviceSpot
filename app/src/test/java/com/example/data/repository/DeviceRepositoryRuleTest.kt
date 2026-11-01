package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Device
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeviceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canSaveAndFindDevice() = runBlocking {
        val aliceUid = signInTestUser("alice@devicespot.test")
        val repository = DeviceRepository(firestore, aliceUid)

        val newDevice = Device(
            qrCode = "TEST-QR-100",
            name = "Digital Multimeter",
            model = "Fluke 87V",
            category = "Testing & Lab Tools",
            storageLocation = "Lab Shelf 1A",
            currentLocation = "Lab Shelf 1A",
            status = Device.STATUS_STORED
        )

        val deviceId = withTimeout(AUTH_TIMEOUT_MS) {
            repository.saveDevice(newDevice)
        }
        assertTrue("Device ID should be non-empty", deviceId.isNotBlank())

        val found = withTimeout(AUTH_TIMEOUT_MS) {
            repository.findDeviceByQr("TEST-QR-100")
        }
        assertNotNull("Should find created device by QR", found)
        assertEquals("Digital Multimeter", found?.name)
    }

    @Test
    fun crossUserAccess_bobCannotAccessAliceDevices() = runBlocking {
        val aliceUid = signInTestUser("alice_cross@devicespot.test")
        val aliceRepo = DeviceRepository(firestore, aliceUid)

        val deviceId = withTimeout(AUTH_TIMEOUT_MS) {
            aliceRepo.saveDevice(
                Device(
                    qrCode = "ALICE-SECRET-01",
                    name = "Alice Lab Oscilloscope",
                    category = "Testing & Lab Tools",
                    storageLocation = "Lab Shelf 1A",
                    currentLocation = "Lab Shelf 1A"
                )
            )
        }

        // Now switch to Bob
        signInTestUser("bob_cross@devicespot.test")

        try {
            // Bob directly tries to read Alice's collection
            firestore.collection("users").document(aliceUid).collection("devices").document(deviceId).get().await()
            fail("Expected security rules to reject Bob accessing Alice's devices")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Should throw FirebaseFirestoreException", firestoreException)
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                firestoreException?.code
            )
        }
    }

    @Test
    fun unauthenticatedAccess_rejectedBySecurityRules() = runBlocking {
        val aliceUid = signInTestUser("alice_unauth@devicespot.test")
        val aliceRepo = DeviceRepository(firestore, aliceUid)

        withTimeout(AUTH_TIMEOUT_MS) {
            aliceRepo.saveDevice(
                Device(
                    qrCode = "ALICE-DEVICE-99",
                    name = "Alice Drone Kit",
                    category = "Drones & Robotics",
                    storageLocation = "Pelican Case #01",
                    currentLocation = "Pelican Case #01"
                )
            )
        }

        // Sign out
        auth.signOut()

        try {
            firestore.collection("users").document(aliceUid).collection("devices").get().await()
            fail("Expected unauthenticated query to be rejected by security rules")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Should be a FirebaseFirestoreException", firestoreException)
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                firestoreException?.code
            )
        }
    }
}
