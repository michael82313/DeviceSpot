package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Device
import com.example.data.model.DeviceLocationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY lastUpdated DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    fun getDeviceById(id: Long): Flow<Device?>

    @Query("SELECT * FROM devices WHERE qrCode = :qrCode LIMIT 1")
    fun getDeviceByQrCode(qrCode: String): Flow<Device?>

    @Query("SELECT * FROM devices WHERE qrCode = :qrCode LIMIT 1")
    suspend fun findDeviceByQrCodeSync(qrCode: String): Device?

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun findDeviceByIdSync(id: Long): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DeviceLocationLog): Long

    @Query("SELECT * FROM device_location_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getLogsForDevice(deviceId: Long): Flow<List<DeviceLocationLog>>

    @Query("SELECT * FROM device_location_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<DeviceLocationLog>>

    @Query("SELECT DISTINCT storageLocation FROM devices UNION SELECT DISTINCT currentLocation FROM devices")
    fun getDistinctLocations(): Flow<List<String>>
}
