package com.example.data.repository

import android.util.Log
import com.example.data.model.Device
import com.example.data.model.DeviceLocationLog
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DeviceRepository(
    private val db: FirebaseFirestore,
    private val userId: String
) {
    private val devicesCollection = db.collection("users").document(userId).collection("devices")
    private val logsCollection = db.collection("users").document(userId).collection("device_location_logs")

    val allDevices: Flow<List<Device>> = callbackFlow {
        val listener = devicesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, devicesCollection.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Device::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w("DeviceRepository", "Failed to deserialize device ${doc.id}", e)
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    val allLogs: Flow<List<DeviceLocationLog>> = callbackFlow {
        val listener = logsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, logsCollection.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(DeviceLocationLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w("DeviceRepository", "Failed to deserialize log ${doc.id}", e)
                        null
                    }
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun getLogsForDevice(deviceId: String): Flow<List<DeviceLocationLog>> = callbackFlow {
        val listener = logsCollection
            .whereEqualTo("deviceId", deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, logsCollection.path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DeviceLocationLog::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun findDeviceByQr(qrCode: String): Device? {
        val trimmed = qrCode.trim()
        val querySnapshot = devicesCollection
            .whereEqualTo("qrCode", trimmed)
            .limit(1)
            .get()
            .await()
        return querySnapshot.documents.firstOrNull()?.let { doc ->
            doc.toObject(Device::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun saveDevice(device: Device): String {
        val isNew = device.id.isBlank()
        val docRef = if (isNew) devicesCollection.document() else devicesCollection.document(device.id)
        val finalDeviceId = docRef.id

        if (isNew) {
            val deviceData = mutableMapOf<String, Any>(
                "userId" to userId,
                "qrCode" to device.qrCode.trim(),
                "name" to device.name.trim(),
                "category" to device.category.ifBlank { "General Hardware" },
                "storageLocation" to device.storageLocation.ifBlank { "Main Storage" },
                "currentLocation" to device.currentLocation.ifBlank { "Main Storage" },
                "status" to device.status.ifBlank { Device.STATUS_STORED },
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (device.model.isNotBlank()) deviceData["model"] = device.model.trim()
            if (device.assignedTo.isNotBlank()) deviceData["assignedTo"] = device.assignedTo.trim()
            if (device.notes.isNotBlank()) deviceData["notes"] = device.notes.trim()
            if (device.colorTag.isNotBlank()) deviceData["colorTag"] = device.colorTag.trim()

            docRef.set(deviceData).await()

            val logRef = logsCollection.document()
            val logData = mutableMapOf<String, Any>(
                "userId" to userId,
                "deviceId" to finalDeviceId,
                "deviceQrCode" to device.qrCode.trim(),
                "deviceName" to device.name.trim(),
                "fromLocation" to "None (Registered)",
                "toLocation" to device.currentLocation.trim(),
                "action" to "Registered Device",
                "actor" to device.assignedTo.ifBlank { "Owner" },
                "note" to "Initial registration with QR/Barcode ${device.qrCode}",
                "createdAt" to FieldValue.serverTimestamp()
            )
            logRef.set(logData).await()
        } else {
            val updateData = mutableMapOf<String, Any>(
                "qrCode" to device.qrCode.trim(),
                "name" to device.name.trim(),
                "category" to device.category.ifBlank { "General Hardware" },
                "storageLocation" to device.storageLocation.ifBlank { "Main Storage" },
                "currentLocation" to device.currentLocation.ifBlank { "Main Storage" },
                "status" to device.status.ifBlank { Device.STATUS_STORED },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (device.model.isNotBlank()) updateData["model"] = device.model.trim()
            if (device.assignedTo.isNotBlank()) updateData["assignedTo"] = device.assignedTo.trim()
            if (device.notes.isNotBlank()) updateData["notes"] = device.notes.trim()
            if (device.colorTag.isNotBlank()) updateData["colorTag"] = device.colorTag.trim()

            docRef.update(updateData).await()
        }
        return finalDeviceId
    }

    suspend fun updateLocation(
        device: Device,
        newLocation: String,
        newStatus: String,
        actor: String = "",
        note: String = ""
    ) {
        val oldLocation = device.currentLocation
        val docRef = devicesCollection.document(device.id)

        val updateData = mutableMapOf<String, Any>(
            "currentLocation" to newLocation.trim(),
            "status" to newStatus,
            "assignedTo" to if (newStatus == Device.STATUS_STORED) "" else actor.ifBlank { device.assignedTo },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updateData).await()

        val actionName = when {
            newStatus == Device.STATUS_STORED -> "Returned to Storage"
            newStatus == Device.STATUS_CHECKED_OUT -> "Checked Out"
            newLocation != oldLocation -> "Moved & Left Last Here"
            else -> "Status Updated"
        }

        val logRef = logsCollection.document()
        val logData = mutableMapOf<String, Any>(
            "userId" to userId,
            "deviceId" to device.id,
            "deviceQrCode" to device.qrCode,
            "deviceName" to device.name,
            "fromLocation" to oldLocation,
            "toLocation" to newLocation.trim(),
            "action" to actionName,
            "actor" to actor.ifBlank { device.assignedTo },
            "note" to note.ifBlank { "Location tracked via DeviceSpot" },
            "createdAt" to FieldValue.serverTimestamp()
        )
        logRef.set(logData).await()
    }

    suspend fun deleteDevice(device: Device) {
        if (device.id.isNotBlank()) {
            devicesCollection.document(device.id).delete().await()
        }
    }

    suspend fun deleteAllUserData() {
        try {
            val devDocs = devicesCollection.get().await()
            for (doc in devDocs.documents) {
                doc.reference.delete().await()
            }
            val logDocs = logsCollection.get().await()
            for (doc in logDocs.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w("DeviceRepository", "Failed to clean up user data before deletion", e)
        }
    }

    suspend fun seedInitialDevicesIfEmpty() {
        try {
            val snapshot = devicesCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val sampleDevices = listOf(
                    Device(
                        qrCode = "QR-DEV-001-LAB",
                        name = "Siglent SDS1104X-E 4-Ch Oscilloscope",
                        model = "SDS1104X-E",
                        category = "Testing & Lab Tools",
                        storageLocation = "Lab Shelf 1A",
                        currentLocation = "Workbench Pegboard",
                        status = Device.STATUS_IN_USE,
                        assignedTo = "Hardware Team",
                        notes = "100MHz 4-channel scope with 4 passive probes and BNC cables",
                        colorTag = "#00E5FF"
                    ),
                    Device(
                        qrCode = "QR-DEV-002-CAM",
                        name = "Sony FX3 Cinema Camera Kit",
                        model = "ILME-FX3",
                        category = "Cameras & Optics",
                        storageLocation = "Pelican Case #01",
                        currentLocation = "Pelican Case #01",
                        status = Device.STATUS_STORED,
                        assignedTo = "",
                        notes = "Includes XLR top handle, 2x 160GB CFexpress cards, 3x NP-FZ100 batteries",
                        colorTag = "#FFB300"
                    ),
                    Device(
                        qrCode = "QR-DEV-003-LAP",
                        name = "ThinkPad P16 Gen 2 Workstation",
                        model = "21FA001KUS",
                        category = "Laptops & Computers",
                        storageLocation = "Office Desk Drawer",
                        currentLocation = "Conference Room Cart",
                        status = Device.STATUS_CHECKED_OUT,
                        assignedTo = "Alex Rivera",
                        notes = "i9-13980HX, 64GB ECC, RTX 3500 Ada, 230W Slim Tip AC charger included",
                        colorTag = "#7C4DFF"
                    ),
                    Device(
                        qrCode = "012345678905",
                        name = "FLIR E8-XT Infrared Thermal Camera",
                        model = "E8-XT",
                        category = "Testing & Lab Tools",
                        storageLocation = "Storage Bin A (Hardware)",
                        currentLocation = "Storage Bin A (Hardware)",
                        status = Device.STATUS_STORED,
                        assignedTo = "",
                        notes = "Calibrated thermal sensor with MSX enhancement, spare battery and transport pouch",
                        colorTag = "#00E676"
                    ),
                    Device(
                        qrCode = "QR-DEV-005-DRN",
                        name = "DJI Mavic 3 Pro Cine Drone",
                        model = "M3P-CINE",
                        category = "Drones & Robotics",
                        storageLocation = "Pelican Case #01",
                        currentLocation = "Pelican Case #01",
                        status = Device.STATUS_STORED,
                        assignedTo = "",
                        notes = "Triple-camera system, RC Pro remote controller, Fly More 3-battery charging hub",
                        colorTag = "#FF5252"
                    )
                )
                for (sample in sampleDevices) {
                    saveDevice(sample)
                }
            }
        } catch (e: Exception) {
            Log.w("DeviceRepository", "Initial sample seeding skipped or failed", e)
        }
    }
}
