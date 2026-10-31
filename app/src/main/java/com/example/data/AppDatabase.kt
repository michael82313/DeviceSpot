package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DeviceDao
import com.example.data.model.Device
import com.example.data.model.DeviceLocationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Device::class, DeviceLocationLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "device_spot_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialDevices(database.deviceDao())
                    }
                }
            }
        }

        private suspend fun populateInitialDevices(dao: DeviceDao) {
            val now = System.currentTimeMillis()
            val hourAgo = now - 3600_000L * 2
            val yesterday = now - 86400_000L

            val initialDevices = listOf(
                Device(
                    id = 1,
                    qrCode = "DEV-1082",
                    name = "MacBook Pro 16\" (M2 Max)",
                    model = "Apple Silicon 32GB 1TB",
                    category = "Laptops & Computers",
                    storageLocation = "Office Desk Drawer",
                    currentLocation = "Conference Room B",
                    status = Device.STATUS_IN_USE,
                    assignedTo = "Alex Vance",
                    notes = "Has USB-C MagSafe cable and 140W brick. Left on presenter desk after all-hands.",
                    colorTag = "#00E5FF",
                    lastUpdated = hourAgo,
                    createdAt = yesterday
                ),
                Device(
                    id = 2,
                    qrCode = "DEV-2041",
                    name = "Sony Alpha A7 IV Camera",
                    model = "ILCE-7M4 with 24-70mm GM II",
                    category = "Cameras & Optics",
                    storageLocation = "Pelican Case #01",
                    currentLocation = "Pelican Case #01",
                    status = Device.STATUS_STORED,
                    assignedTo = "",
                    notes = "Includes 2 extra NP-FZ100 batteries and 128GB Tough SD card. Cleaned and locked.",
                    colorTag = "#FFB703",
                    lastUpdated = yesterday,
                    createdAt = yesterday
                ),
                Device(
                    id = 3,
                    qrCode = "DEV-3099",
                    name = "DJI Mini 4 Pro Drone Kit",
                    model = "Fly More Combo + RC 2 Screen",
                    category = "Drones & Robotics",
                    storageLocation = "Lab Shelf 2B",
                    currentLocation = "Lab Shelf 2B",
                    status = Device.STATUS_STORED,
                    assignedTo = "",
                    notes = "All 3 batteries at 60% storage charge. Spare props inside mesh pocket.",
                    colorTag = "#10B981",
                    lastUpdated = now - 1800_000L,
                    createdAt = yesterday
                ),
                Device(
                    id = 4,
                    qrCode = "DEV-4105",
                    name = "Fluke 87V Industrial Multimeter",
                    model = "True RMS High Accuracy",
                    category = "Testing & Lab Tools",
                    storageLocation = "Workbench Pegboard",
                    currentLocation = "Workbench Pegboard",
                    status = Device.STATUS_STORED,
                    assignedTo = "",
                    notes = "Includes silicone test leads and alligator clips. Calibrated last month.",
                    colorTag = "#F59E0B",
                    lastUpdated = yesterday,
                    createdAt = yesterday
                ),
                Device(
                    id = 5,
                    qrCode = "DEV-5512",
                    name = "iPad Pro 12.9\" (M2)",
                    model = "WiFi + Cellular 256GB Space Gray",
                    category = "Mobile & Tablets",
                    storageLocation = "Lab Shelf 1A",
                    currentLocation = "Office Desk Drawer",
                    status = Device.STATUS_CHECKED_OUT,
                    assignedTo = "Sarah Connor",
                    notes = "Has Apple Pencil 2 attached. Borrowed for client UI demo.",
                    colorTag = "#8B5CF6",
                    lastUpdated = now - 5400_000L,
                    createdAt = yesterday
                ),
                Device(
                    id = 6,
                    qrCode = "DEV-6019",
                    name = "Anker 737 Power Bank (24k)",
                    model = "140W PowerCore 24,000mAh",
                    category = "Power & Chargers",
                    storageLocation = "Storage Bin A (Hardware)",
                    currentLocation = "Backpack North Face",
                    status = Device.STATUS_IN_USE,
                    assignedTo = "Michael",
                    notes = "Left in travel backpack after field test trip. Fully charged.",
                    colorTag = "#EC4899",
                    lastUpdated = now - 7200_000L,
                    createdAt = yesterday
                )
            )

            initialDevices.forEach { device ->
                dao.insertDevice(device)
            }

            // Seed initial audit log entries
            val initialLogs = listOf(
                DeviceLocationLog(
                    deviceId = 1,
                    deviceQrCode = "DEV-1082",
                    deviceName = "MacBook Pro 16\" (M2 Max)",
                    fromLocation = "Office Desk Drawer",
                    toLocation = "Conference Room B",
                    action = "Moved & Left Last Here",
                    actor = "Alex Vance",
                    timestamp = hourAgo,
                    note = "Left connected to presentation monitor."
                ),
                DeviceLocationLog(
                    deviceId = 5,
                    deviceQrCode = "DEV-5512",
                    deviceName = "iPad Pro 12.9\" (M2)",
                    fromLocation = "Lab Shelf 1A",
                    toLocation = "Office Desk Drawer",
                    action = "Checked Out",
                    actor = "Sarah Connor",
                    timestamp = now - 5400_000L,
                    note = "Needed for design review session."
                ),
                DeviceLocationLog(
                    deviceId = 6,
                    deviceQrCode = "DEV-6019",
                    deviceName = "Anker 737 Power Bank (24k)",
                    fromLocation = "Storage Bin A (Hardware)",
                    toLocation = "Backpack North Face",
                    action = "Moved Location",
                    actor = "Michael",
                    timestamp = now - 7200_000L,
                    note = "Packed for mobile work."
                ),
                DeviceLocationLog(
                    deviceId = 3,
                    deviceQrCode = "DEV-3099",
                    deviceName = "DJI Mini 4 Pro Drone Kit",
                    fromLocation = "Field Testing Ground",
                    toLocation = "Lab Shelf 2B",
                    action = "Returned & Verified",
                    actor = "Alex",
                    timestamp = now - 1800_000L,
                    note = "Checked in via QR code scan."
                )
            )

            initialLogs.forEach { log ->
                dao.insertLog(log)
            }
        }
    }
}
