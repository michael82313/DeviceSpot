package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Device(
    @DocumentId val id: String = "",
    val userId: String = "",
    val qrCode: String = "",
    val name: String = "",
    val model: String = "",
    val category: String = "General Hardware",
    val storageLocation: String = "Main Storage",
    val currentLocation: String = "Main Storage",
    val status: String = STATUS_STORED,
    val assignedTo: String = "",
    val notes: String = "",
    val colorTag: String = "#00E5FF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val lastUpdated: Long
        get() = (updatedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()

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
            "Storage Bins & Boxes",
            "Other"
        )

        val DEFAULT_LOCATIONS = listOf(
            "Office Desk Drawer",
        )
    }
}

@IgnoreExtraProperties
data class DeviceLocationLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceQrCode: String = "",
    val deviceName: String = "",
    val fromLocation: String = "",
    val toLocation: String = "",
    val action: String = "",
    val actor: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val timestamp: Long
        get() = createdAt?.toDate()?.time ?: System.currentTimeMillis()
}
