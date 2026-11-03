package com.example.ai

import android.util.Log
import com.example.data.model.Device
import com.example.data.model.DeviceLocationLog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "DeviceAiAssistant"

data class DeviceAiSuggestion(
    val category: String,
    val recommendedStorage: String,
    val recommendedLocationReason: String,
    val accessoriesToBundle: List<String>,
    val handlingTip: String
)

class DeviceAiAssistant {

    // Structured JSON schema for device auto-categorization & storage advice
    private val deviceSuggestionSchema = Schema.obj(
        properties = mapOf(
            "category" to Schema.string(description = "Closest device category from: Laptops & Computers, Cameras & Optics, Audio & Video, Testing & Lab Tools, Mobile & Tablets, Drones & Robotics, Power & Chargers, Cables & Adapters, Storage Bins & Boxes, Other"),
            "recommendedStorage" to Schema.string(description = "Specific ideal storage location name (e.g. Pelican Case #01, Lab Shelf 2B, ESD Bin, Office Desk Drawer)"),
            "recommendedLocationReason" to Schema.string(description = "Short reason why this spot is ideal for this hardware"),
            "accessoriesToBundle" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 critical accessories, cables, or peripherals to store alongside it"
            ),
            "handlingTip" to Schema.string(description = "A practical storage or battery maintenance tip for this hardware")
        )
    )

    // Lazy initialization of structured model
    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = deviceSuggestionSchema
                temperature = 0.2f
            }
        )
    }

    // Lazy initialization of chat / Q&A assistant model
    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )
    }

    /**
     * Uses Gemini to analyze a device and recommend optimal storage spot, category, and accessories.
     */
    suspend fun suggestDeviceDetails(
        deviceName: String,
        model: String
    ): Result<DeviceAiSuggestion> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analyze this hardware device and provide categorization, optimal storage placement, and essential accessories.
                Device Name: $deviceName
                Model / Specs: ${model.ifBlank { "Standard specification" }}
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }

            val jsonString = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")

            val json = JSONObject(jsonString)
            val category = json.optString("category", "General Hardware")
            val storage = json.optString("recommendedStorage", "Main Storage Shelf")
            val reason = json.optString("recommendedLocationReason", "Recommended for safe storage.")
            val tip = json.optString("handlingTip", "Store in a dry, cool location.")

            val accessoriesArray = json.optJSONArray("accessoriesToBundle")
            val accessories = mutableListOf<String>()
            if (accessoriesArray != null) {
                for (i in 0 until accessoriesArray.length()) {
                    accessories.add(accessoriesArray.getString(i))
                }
            }

            Result.success(
                DeviceAiSuggestion(
                    category = category,
                    recommendedStorage = storage,
                    recommendedLocationReason = reason,
                    accessoriesToBundle = accessories,
                    handlingTip = tip
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error generating device details suggestion", e)
            Result.failure(e)
        }
    }

    /**
     * Streams responses to user questions regarding device locations, audit logs, and hardware whereabouts.
     */
    fun askLocatorStream(
        userQuestion: String,
        devices: List<Device>,
        recentLogs: List<DeviceLocationLog>
    ): Flow<String> = flow {
        val inventorySummary = buildString {
            appendLine("=== Current Device Inventory ===")
            if (devices.isEmpty()) {
                appendLine("No devices currently registered.")
            } else {
                devices.forEach { d ->
                    appendLine("- [${d.name}] (${d.model.ifBlank { "No model" }}), Category: ${d.category}, Status: ${d.status}, Current Location: ${d.currentLocation}, Storage Location: ${d.storageLocation}, Assigned To: ${d.assignedTo.ifBlank { "None" }}, Barcode/QR: ${d.qrCode}")
                }
            }
            appendLine("\n=== Recent Movement & Location Logs (Latest 20) ===")
            if (recentLogs.isEmpty()) {
                appendLine("No recent movement logs.")
            } else {
                recentLogs.take(20).forEach { log ->
                    appendLine("- Device: ${log.deviceName.ifBlank { log.deviceId }}, ${log.fromLocation} -> ${log.toLocation}, Action: ${log.action}, By: ${log.actor}, Note: ${log.note.ifBlank { "None" }}")
                }
            }
        }

        val prompt = """
            You are DeviceSpot Locator AI, an intelligent hardware asset management assistant.
            Use the current inventory and recent audit logs below to answer the user's question accurately.
            Be concise, helpful, and reference the specific device name and current location.

            $inventorySummary

            User Question: $userQuestion
        """.trimIndent()

        val responseStream = assistantModel.generateContentStream(prompt)
        responseStream.collect { chunk ->
            val text = chunk.text
            if (!text.isNullOrEmpty()) {
                emit(text)
            }
        }
    }.flowOn(Dispatchers.IO)
}
