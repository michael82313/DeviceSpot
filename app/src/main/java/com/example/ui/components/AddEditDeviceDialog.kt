package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Device
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditDeviceDialog(
    initialDevice: Device?,
    prefillQrCode: String?,
    availableLocations: List<String>,
    aiSuggestionState: DeviceViewModel.AiSuggestionUiState,
    onRequestAiSuggestion: (String, String) -> Unit,
    onResetAiSuggestion: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Device) -> Unit
) {
    val isEdit = initialDevice != null

    var name by remember { mutableStateOf(initialDevice?.name ?: "") }
    var model by remember { mutableStateOf(initialDevice?.model ?: "") }
    var qrCode by remember {
        mutableStateOf(
            initialDevice?.qrCode
                ?: prefillQrCode
                ?: QrCodeGenerator.generateUniqueCode()
        )
    }
    var category by remember {
        mutableStateOf(initialDevice?.category ?: Device.DEFAULT_CATEGORIES.first())
    }
    var storageLocation by remember {
        mutableStateOf(initialDevice?.storageLocation ?: "Lab Shelf 1A")
    }
    var currentLocation by remember {
        mutableStateOf(initialDevice?.currentLocation ?: initialDevice?.storageLocation ?: "Lab Shelf 1A")
    }
    var status by remember {
        mutableStateOf(initialDevice?.status ?: Device.STATUS_STORED)
    }
    var assignedTo by remember { mutableStateOf(initialDevice?.assignedTo ?: "") }
    var notes by remember { mutableStateOf(initialDevice?.notes ?: "") }
    var colorTag by remember { mutableStateOf(initialDevice?.colorTag ?: "#00E5FF") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // React to Gemini AI suggestions
    LaunchedEffect(aiSuggestionState) {
        if (aiSuggestionState is DeviceViewModel.AiSuggestionUiState.Success) {
            val sug = aiSuggestionState.suggestion
            if (sug.category.isNotBlank()) {
                category = sug.category
            }
            if (sug.recommendedStorage.isNotBlank()) {
                storageLocation = sug.recommendedStorage
                if (!isEdit) {
                    currentLocation = sug.recommendedStorage
                }
            }
        }
    }

    val colorOptions = listOf(
        "#00E5FF", // Cyan
        "#FFB703", // Amber
        "#10B981", // Emerald
        "#8B5CF6", // Violet
        "#EC4899", // Pink
        "#3B82F6", // Blue
        "#F97316"  // Orange
    )

    Dialog(
        onDismissRequest = {
            onResetAiSuggestion()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("add_edit_device_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Edit Device" else "Register Device with QR/Barcode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = {
                            onResetAiSuggestion()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_add_edit_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR / Barcode Code Field with Generate Button
                OutlinedTextField(
                    value = qrCode,
                    onValueChange = { qrCode = it },
                    label = { Text("QR / Barcode Identifier") },
                    placeholder = { Text("e.g. DEV-4921, 012345678905") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { qrCode = QrCodeGenerator.generateUniqueCode() },
                            modifier = Modifier.testTag("generate_qr_code_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Generate New Code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_qr_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Device Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Device Name *") },
                    placeholder = { Text("e.g. Sony Alpha A7 IV, DJI Mini 4 Pro, MacBook Pro") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Model / Serial (Optional)
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model / Serial / Hardware Tag") },
                    placeholder = { Text("e.g. M2 Max 32GB / S/N: C02G90...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_model_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Gemini AI Suggestion Button & Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini AI Assistant",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            FilledTonalButton(
                                onClick = { onRequestAiSuggestion(name, model) },
                                enabled = name.isNotBlank() && aiSuggestionState !is DeviceViewModel.AiSuggestionUiState.Loading,
                                modifier = Modifier.testTag("ai_suggest_device_btn")
                            ) {
                                if (aiSuggestionState is DeviceViewModel.AiSuggestionUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyzing...", fontSize = 11.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Suggest Spot", fontSize = 11.sp)
                                }
                            }
                        }

                        when (aiSuggestionState) {
                            is DeviceViewModel.AiSuggestionUiState.Success -> {
                                val sug = aiSuggestionState.suggestion
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Recommended Spot: ${sug.recommendedStorage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = sug.recommendedLocationReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                if (sug.accessoriesToBundle.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.clickable {
                                            val addNotes = "Accessories: ${sug.accessoriesToBundle.joinToString(", ")}. Tip: ${sug.handlingTip}"
                                            notes = if (notes.isBlank()) addNotes else "$notes\n$addNotes"
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add suggested accessories & maintenance tip to notes", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                            is DeviceViewModel.AiSuggestionUiState.Error -> {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiSuggestionState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                            else -> {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Device.DEFAULT_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("category_chip_${cat.replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Home Storage Location
                Text(
                    text = "Home Storage Location *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Where this device belongs when not in use",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storageLocation,
                    onValueChange = {
                        storageLocation = it
                        if (!isEdit) currentLocation = it
                    },
                    label = { Text("Default Storage Spot") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_storage_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val popularLocations = (availableLocations + Device.DEFAULT_LOCATIONS).distinct().take(6)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    popularLocations.forEach { loc ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                storageLocation = loc
                                if (!isEdit) currentLocation = loc
                            }
                        ) {
                            Text(
                                text = loc,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Location (Where left last)
                Text(
                    text = "Current / Left Last Location",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentLocation,
                    onValueChange = { currentLocation = it },
                    label = { Text("Currently Stored or Left At") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_current_location_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Selection
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Device.ALL_STATUSES.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) },
                            modifier = Modifier.testTag("status_chip_${st.replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Assigned to person / borrower
                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text("Assigned To / Borrower (Optional)") },
                    placeholder = { Text("e.g. Michael, Lab Team") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_assigned_to_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color accent tag
                Text(
                    text = "Color Badge Tag",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { hex ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorTag.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorTag = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Accessories Included") },
                    placeholder = { Text("e.g. Includes charger, warranty until 2027, firmware 2.0") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_notes_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetAiSuggestion()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("cancel_save_device_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Device name is required"
                                return@Button
                            }
                            if (qrCode.isBlank()) {
                                qrCode = QrCodeGenerator.generateUniqueCode()
                            }
                            val updatedDevice = (initialDevice ?: Device(qrCode = qrCode, name = name)).copy(
                                name = name.trim(),
                                model = model.trim(),
                                qrCode = qrCode.trim(),
                                category = category,
                                storageLocation = storageLocation.trim().ifBlank { "Main Storage" },
                                currentLocation = currentLocation.trim().ifBlank { storageLocation.trim() },
                                status = status,
                                assignedTo = assignedTo.trim(),
                                notes = notes.trim(),
                                colorTag = colorTag,
                                lastUpdated = System.currentTimeMillis()
                            )
                            onResetAiSuggestion()
                            onSave(updatedDevice)
                        },
                        modifier = Modifier.testTag("confirm_save_device_btn")
                    ) {
                        Text(if (isEdit) "Save Changes" else "Register Device")
                    }
                }
            }
        }
    }
}
