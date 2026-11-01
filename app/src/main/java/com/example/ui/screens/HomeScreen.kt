package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddEditDeviceDialog
import com.example.ui.components.AiLocatorDialog
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.components.QrCodePreviewDialog
import com.example.ui.components.QrScannerSheet
import com.example.ui.components.ScanResultDialog
import com.example.ui.components.UpdateLocationDialog
import com.example.ui.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DeviceViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val filteredDevices by viewModel.filteredDevices.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val distinctLocations by viewModel.distinctLocations.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val selectedLocationFilter by viewModel.selectedLocationFilter.collectAsStateWithLifecycle()

    val scanLookupResult by viewModel.scanLookupResult.collectAsStateWithLifecycle()
    val selectedDeviceForDetail by viewModel.selectedDeviceForDetail.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingDevice by viewModel.editingDevice.collectAsStateWithLifecycle()
    val deviceForLocationUpdate by viewModel.deviceForLocationUpdate.collectAsStateWithLifecycle()
    val deviceForQrPreview by viewModel.deviceForQrPreview.collectAsStateWithLifecycle()
    val prefillQrCode by viewModel.prefillQrCode.collectAsStateWithLifecycle()

    // Gemini AI states
    val aiSuggestionState by viewModel.aiSuggestionState.collectAsStateWithLifecycle()
    val isAiLocatorOpen by viewModel.isAiLocatorOpen.collectAsStateWithLifecycle()
    val aiLocatorResponse by viewModel.aiLocatorResponse.collectAsStateWithLifecycle()
    val isAiLocatorStreaming by viewModel.isAiLocatorStreaming.collectAsStateWithLifecycle()
    val aiLocatorError by viewModel.aiLocatorError.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var isScannerOpen by remember { mutableStateOf(false) }
    var isAccountDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DeviceSpot",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Gemini AI Locator Button
                    IconButton(
                        onClick = { viewModel.openAiLocator() },
                        modifier = Modifier.testTag("top_bar_gemini_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask Gemini Hardware Locator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Scanner Button
                    IconButton(
                        onClick = { isScannerOpen = true },
                        modifier = Modifier.testTag("top_bar_scan_qr_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR or Barcode",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Add Button
                    IconButton(
                        onClick = { viewModel.openAddDevice() },
                        modifier = Modifier.testTag("top_bar_add_device_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Device"
                        )
                    }
                    // Account & Multi-Device Sync Button
                    IconButton(
                        onClick = { isAccountDialogOpen = true },
                        modifier = Modifier.testTag("top_bar_account_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account & Multi-Device Sync",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(imageVector = Icons.Default.Devices, contentDescription = "Devices")
                    },
                    label = { Text("Devices") },
                    modifier = Modifier.testTag("nav_item_devices")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(imageVector = Icons.Default.Place, contentDescription = "Locations")
                    },
                    label = { Text("Locations") },
                    modifier = Modifier.testTag("nav_item_locations")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Audit Log")
                    },
                    label = { Text("Audit Log") },
                    modifier = Modifier.testTag("nav_item_audit_log")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { isScannerOpen = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null
                        )
                    },
                    text = { Text("Scan Code", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("home_scan_fab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                0 -> DevicesTab(
                    devices = filteredDevices,
                    totalDeviceCount = allDevices.size,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelect(it) },
                    selectedStatus = selectedStatus,
                    onStatusSelect = { viewModel.onStatusSelect(it) },
                    selectedLocation = selectedLocationFilter,
                    onLocationSelect = { viewModel.onLocationFilterSelect(it) },
                    onDeviceClick = { viewModel.openDeviceDetail(it) },
                    onUpdateSpotClick = { viewModel.openUpdateLocation(it) },
                    onShowQrClick = { viewModel.openQrPreview(it) },
                    onAddNewDevice = { viewModel.openAddDevice() }
                )
                1 -> LocationsTab(
                    devices = allDevices,
                    onDeviceClick = { viewModel.openDeviceDetail(it) },
                    onLocationFilterSelect = { location ->
                        viewModel.onLocationFilterSelect(location)
                        currentTab = 0
                    }
                )
                2 -> AuditLogTab(
                    logs = allLogs,
                    onDeviceClick = { log ->
                        val dev = allDevices.find { it.id == log.deviceId }
                        if (dev != null) {
                            viewModel.openDeviceDetail(dev)
                        }
                    }
                )
            }
        }
    }

    // Modal: Full Camera QR & Barcode Scanner Bottom Sheet
    if (isScannerOpen) {
        QrScannerSheet(
            devices = allDevices,
            onDismiss = { isScannerOpen = false },
            onCodeScanned = { rawCode, formatName ->
                isScannerOpen = false
                viewModel.onQrScanned(rawCode, formatName)
            }
        )
    }

    // Modal: Lookup Result Dialog
    scanLookupResult?.let { result ->
        ScanResultDialog(
            result = result,
            onDismiss = { viewModel.clearScanResult() },
            onViewDevice = { dev ->
                viewModel.clearScanResult()
                viewModel.openDeviceDetail(dev)
            },
            onUpdateLocation = { dev ->
                viewModel.clearScanResult()
                viewModel.openUpdateLocation(dev)
            },
            onRegisterNew = { rawQr ->
                viewModel.clearScanResult()
                viewModel.openAddDevice(initialQr = rawQr)
            }
        )
    }

    // Modal: Device Detail Sheet
    selectedDeviceForDetail?.let { device ->
        DeviceDetailSheet(
            device = device,
            logsFlow = viewModel.getLogsForDevice(device.id),
            onDismiss = { viewModel.closeDeviceDetail() },
            onEdit = {
                viewModel.closeDeviceDetail()
                viewModel.openEditDevice(device)
            },
            onUpdateLocation = {
                viewModel.closeDeviceDetail()
                viewModel.openUpdateLocation(device)
            },
            onQuickReturnToStorage = {
                viewModel.quickReturnToStorage(device)
            },
            onShowQr = {
                viewModel.openQrPreview(device)
            },
            onDelete = {
                viewModel.deleteDevice(device)
            }
        )
    }

    // Modal: Add / Edit Device Dialog
    if (isAddEditOpen) {
        AddEditDeviceDialog(
            editingDevice = editingDevice,
            initialQr = prefillQrCode,
            availableLocations = distinctLocations,
            aiSuggestionState = aiSuggestionState,
            onRequestAiSuggestion = { name, model ->
                viewModel.requestAiDeviceSuggestion(name, model)
            },
            onResetAiSuggestion = { viewModel.resetAiSuggestion() },
            onDismiss = { viewModel.closeAddEdit() },
            onSave = { updatedDevice -> viewModel.saveDevice(updatedDevice) }
        )
    }

    // Modal: Update Location Dialog
    deviceForLocationUpdate?.let { dev ->
        UpdateLocationDialog(
            device = dev,
            availableLocations = distinctLocations,
            onDismiss = { viewModel.closeUpdateLocation() },
            onConfirm = { newLoc, newStatus, actor, note ->
                viewModel.updateLocation(dev, newLoc, newStatus, actor, note)
            }
        )
    }

    // Modal: QR Code Printable Preview
    deviceForQrPreview?.let { dev ->
        QrCodePreviewDialog(
            device = dev,
            onDismiss = { viewModel.closeQrPreview() }
        )
    }

    // Modal: Gemini AI Hardware Locator Q&A Dialog
    if (isAiLocatorOpen) {
        AiLocatorDialog(
            response = aiLocatorResponse,
            isStreaming = isAiLocatorStreaming,
            errorMessage = aiLocatorError,
            onDismiss = { viewModel.closeAiLocator() },
            onAsk = { q -> viewModel.askAiLocator(q) }
        )
    }

    // Modal: Account & Multi-Device Sync Dialog
    if (isAccountDialogOpen) {
        AccountSyncDialog(
            userEmail = viewModel.userEmail,
            userDisplayName = viewModel.userDisplayName,
            deviceCount = allDevices.size,
            onSignOut = onSignOut,
            onDismiss = { isAccountDialogOpen = false }
        )
    }
}

@Composable
fun AccountSyncDialog(
    userEmail: String,
    userDisplayName: String,
    deviceCount: Int,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Account & Cloud Sync",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = userDisplayName.ifBlank { "Signed In User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00E676),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Live Multi-Device Sync: Active",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$deviceCount items synced across all your devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "All your QR codes, barcodes, storage shelves, and movement history are backed up and live-synced in real-time across all your phones, tablets, and workstations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("account_dialog_close_btn")
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out")
            }
        }
    )
}
