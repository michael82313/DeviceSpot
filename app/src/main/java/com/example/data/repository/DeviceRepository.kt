package com.example.data.repository

import com.example.data.dao.DeviceDao
import com.example.data.model.Device
import com.example.data.model.DeviceLocationLog
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val dao: DeviceDao) {

    val allDevices: Flow<List<Device>> = dao.getAllDevices()
    val allLogs: Flow<List<DeviceLocationLog>> = dao.getAllLogs()
    val distinctLocations: Flow<List<String>> = dao.getDistinctLocations()

    fun getDeviceById(id: Long): Flow<Device?> = dao.getDeviceById(id)

    fun getDeviceByQr(qrCode: String): Flow<Device?> = dao.getDeviceByQrCode(qrCode)

    suspend fun findDeviceByQr(qrCode: String): Device? = dao.findDeviceByQrCodeSync(qrCode.trim())

    fun getLogsForDevice(deviceId: Long): Flow<List<DeviceLocationLog>> =
        dao.getLogsForDevice(deviceId)

    suspend fun saveDevice(device: Device): Long {
        val isNew = device.id == 0L
        val savedId = dao.insertDevice(device)
        val finalDeviceId = if (isNew) savedId else device.id

        if (isNew) {
            dao.insertLog(
                DeviceLocationLog(
                    deviceId = finalDeviceId,
                    deviceQrCode = device.qrCode,
                    deviceName = device.name,
                    fromLocation = "None (Registered)",
                    toLocation = device.currentLocation,
                    action = "Registered Device",
                    actor = device.assignedTo.ifBlank { "Admin" },
                    timestamp = System.currentTimeMillis(),
                    note = "Initial registration with QR code ${device.qrCode}"
                )
            )
        }
        return savedId
    }

    suspend fun updateLocation(
        device: Device,
        newLocation: String,
        newStatus: String,
        actor: String = "",
        note: String = ""
    ) {
        val oldLocation = device.currentLocation
        val updatedDevice = device.copy(
            currentLocation = newLocation.trim(),
            status = newStatus,
            assignedTo = if (newStatus == Device.STATUS_STORED) "" else (actor.ifBlank { device.assignedTo }),
            lastUpdated = System.currentTimeMillis()
        )
        dao.updateDevice(updatedDevice)

        val actionName = when {
            newStatus == Device.STATUS_STORED -> "Returned to Storage"
            newStatus == Device.STATUS_CHECKED_OUT -> "Checked Out"
            newLocation != oldLocation -> "Moved & Left Last Here"
            else -> "Status Updated"
        }

        dao.insertLog(
            DeviceLocationLog(
                deviceId = device.id,
                deviceQrCode = device.qrCode,
                deviceName = device.name,
                fromLocation = oldLocation,
                toLocation = newLocation.trim(),
                action = actionName,
                actor = actor.ifBlank { device.assignedTo },
                timestamp = System.currentTimeMillis(),
                note = note.ifBlank { "Location tracked via QR Spot" }
            )
        )
    }

    suspend fun deleteDevice(device: Device) {
        dao.deleteDevice(device)
    }
}
