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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.material3.HorizontalDivider
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
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Device
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.PrintTagHelper
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val context = LocalContext.current
    var isBarcodeMode by remember { mutableStateOf(false) }

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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("add_edit_device_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dialog Top Bar (Pinned at top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEdit) "Edit Device" else "Register Device with QR/Barcode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Scrollable form inputs
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {

                // QR / Barcode Code Field with Generate Button
                OutlinedTextField(
                    value = qrCode,
                    onValueChange = { qrCode = it },
                    label = { Text("QR / Barcode Identifier") },
                    placeholder = { Text("e.g. DEV-4921, 012345678905") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    leadingIcon = {
                        Icon(
                            imageVector = if (isBarcodeMode) Icons.Default.ViewWeek else Icons.Default.QrCode,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                qrCode = if (isBarcodeMode) {
                                    QrCodeGenerator.generateRandomBarcode()
                                } else {
                                    QrCodeGenerator.generateRandomQrCode()
                                }
                            },
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

                Spacer(modifier = Modifier.height(8.dp))

                // Generator & Print Controls Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_and_print_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Code Generation & Printing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick Format Selector (QR / Barcode)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = !isBarcodeMode,
                                    onClick = { isBarcodeMode = false },
                                    label = { Text("2D QR", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("select_qr_format_chip")
                                )
                                FilterChip(
                                    selected = isBarcodeMode,
                                    onClick = { isBarcodeMode = true },
                                    label = { Text("1D Barcode", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("select_barcode_format_chip")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Generate Random and Print Tag action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    qrCode = if (isBarcodeMode) {
                                        QrCodeGenerator.generateRandomBarcode()
                                    } else {
                                        QrCodeGenerator.generateRandomQrCode()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("generate_random_code_action_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBarcodeMode) "Random Barcode" else "Random QR",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick = {
                                    if (qrCode.isBlank()) {
                                        qrCode = if (isBarcodeMode) {
                                            QrCodeGenerator.generateRandomBarcode()
                                        } else {
                                            QrCodeGenerator.generateRandomQrCode()
                                        }
                                    }
                                    PrintTagHelper.printAssetTag(
                                        context = context,
                                        deviceTitle = name.ifBlank { "Asset Tag" },
                                        code = qrCode,
                                        location = storageLocation,
                                        isBarcode = isBarcodeMode
                                    )
                                },
                                enabled = qrCode.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("print_generated_code_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Print " + if (isBarcodeMode) "Barcode" else "QR",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        // Live visual preview of the generated code
                        if (qrCode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val previewQrBitmap by produceState<Bitmap?>(initialValue = null, qrCode) {
                                value = withContext(Dispatchers.IO) {
                                    QrCodeGenerator.generateQrBitmap(qrCode, size = 180)
                                }
                            }
                            val previewBarcodeBitmap by produceState<Bitmap?>(initialValue = null, qrCode) {
                                value = withContext(Dispatchers.IO) {
                                    QrCodeGenerator.generate1DBarcodeBitmap(qrCode, width = 360, height = 90)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isBarcodeMode) {
                                            previewBarcodeBitmap?.let { bm ->
                                                Image(
                                                    bitmap = bm.asImageBitmap(),
                                                    contentDescription = "Generated Barcode",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                )
                                            }
                                        } else {
                                            previewQrBitmap?.let { bm ->
                                                Image(
                                                    bitmap = bm.asImageBitmap(),
                                                    contentDescription = "Generated QR Code",
                                                    modifier = Modifier.size(60.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.padding(start = 8.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = qrCode,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = if (isBarcodeMode) "Ready to Print (1D Barcode)" else "Ready to Print (QR Code)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

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

                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Pinned Action Bar at Bottom - Always visible, ample clearance above navigation bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onResetAiSuggestion()
                                onDismiss()
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("cancel_save_device_btn")
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
                                    colorTag = colorTag
                                )
                                onResetAiSuggestion()
                                onSave(updatedDevice)
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("confirm_save_device_btn")
                        ) {
                            Text(
                                text = if (isEdit) "Save Changes" else "Register Device",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
}
