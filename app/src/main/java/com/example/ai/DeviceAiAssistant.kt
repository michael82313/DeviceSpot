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
            modelName = "gemini-3.8-flash",
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
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )
    }

    /**
     * Uses Gemini 3.8 Flash to analyze a device and recommend optimal storage spot, category, and accessories.
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
