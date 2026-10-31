package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices",
    indices = [Index(value = ["qrCode"], unique = true)]
)
data class Device(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val qrCode: String,
    val name: String,
    val model: String = "",
    val category: String = "General Hardware",
    val storageLocation: String = "Main Storage",
    val currentLocation: String = "Main Storage",
    val status: String = STATUS_STORED,
    val assignedTo: String = "",
    val notes: String = "",
    val colorTag: String = "#00E5FF",
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_STORED = "In Storage"
        const val STATUS_IN_USE = "In Use"
        const val STATUS_CHECKED_OUT = "Checked Out"
        const val STATUS_MISSING = "Missing / Search"

        val ALL_STATUSES = listOf(
            STATUS_STORED,
            STATUS_IN_USE,
            STATUS_CHECKED_OUT,
            STATUS_MISSING
        )

        val DEFAULT_CATEGORIES = listOf(
            "Laptops & Computers",
            "Cameras & Optics",
            "Audio & Video",
            "Testing & Lab Tools",
            "Mobile & Tablets",
            "Drones & Robotics",
            "Power & Chargers",
            "Cables & Adapters",
            "Storage Bins & Boxes"
        )

        val DEFAULT_LOCATIONS = listOf(
            "Lab Shelf 1A",
            "Lab Shelf 2B",
            "Office Desk Drawer",
            "Workbench Pegboard",
            "Pelican Case #01",
            "Storage Bin A (Hardware)",
            "Living Room Media Console",
            "Backpack North Face",
            "Conference Room Cart"
        )
    }
}

@Entity(
    tableName = "device_location_logs",
    indices = [Index(value = ["deviceId"]), Index(value = ["deviceQrCode"])]
)
data class DeviceLocationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: Long,
    val deviceQrCode: String,
    val deviceName: String,
    val fromLocation: String,
    val toLocation: String,
    val action: String,
    val actor: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
