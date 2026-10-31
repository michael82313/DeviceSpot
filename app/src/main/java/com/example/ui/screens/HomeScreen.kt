package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
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
                        Text(
                            text = "DeviceSpot",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
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
            ExtendedFloatingActionButton(
                onClick = { isScannerOpen = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Code",
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = { Text("Scan Code", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_scan_qr")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DevicesTab(
                    devices = filteredDevices,
                    allDevicesCount = allDevices.size,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelect(it) },
                    selectedStatus = selectedStatus,
                    onStatusSelect = { viewModel.onStatusSelect(it) },
                    selectedLocationFilter = selectedLocationFilter,
                    onLocationFilterClear = { viewModel.onLocationFilterSelect(null) },
                    onDeviceClick = { viewModel.openDeviceDetail(it) },
                    onUpdateLocationClick = { viewModel.openUpdateLocation(it) },
                    onViewQrClick = { viewModel.openQrPreview(it) },
                    onAddDeviceClick = { viewModel.openAddDevice() }
                )
                1 -> LocationsTab(
                    devices = allDevices,
                    onSelectLocation = { loc ->
                        viewModel.onLocationFilterSelect(loc)
                        currentTab = 0
                    },
                    onDeviceClick = { viewModel.openDeviceDetail(it) }
                )
                2 -> AuditLogTab(
                    logs = allLogs,
                    onLogClick = { log ->
                        val dev = allDevices.find { it.id == log.deviceId }
                        if (dev != null) viewModel.openDeviceDetail(dev)
                    }
                )
            }
        }
    }

    // Modal: QR & Barcode Scanner Sheet
    if (isScannerOpen) {
        QrScannerSheet(
            devicesList = allDevices,
            onDismiss = { isScannerOpen = false },
            onCodeScanned = { scannedCode, formatName ->
                isScannerOpen = false
                viewModel.onQrScanned(scannedCode, formatName)
            }
        )
    }

    // Modal: Scan Result Dialog
    scanLookupResult?.let { result ->
        ScanResultDialog(
            result = result,
            onDismiss = { viewModel.clearScanResult() },
            onUpdateLocation = { dev -> viewModel.openUpdateLocation(dev) },
            onQuickReturnHome = { dev -> viewModel.quickReturnToStorage(dev) },
            onViewDetail = { dev -> viewModel.openDeviceDetail(dev) },
            onRegisterNewDevice = { qr -> viewModel.openAddDevice(initialQr = qr) }
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
            onDelete = { viewModel.deleteDevice(device) },
            onUpdateLocation = {
                viewModel.openUpdateLocation(device)
            },
            onQuickReturnToStorage = {
                viewModel.quickReturnToStorage(device)
            },
            onViewQr = {
                viewModel.openQrPreview(device)
            }
        )
    }

    // Modal: Add or Edit Device Dialog (with Gemini AI suggestion capability)
    if (isAddEditOpen) {
        AddEditDeviceDialog(
            initialDevice = editingDevice,
            prefillQrCode = prefillQrCode,
            availableLocations = distinctLocations,
            aiSuggestionState = aiSuggestionState,
            onRequestAiSuggestion = { name, model -> viewModel.requestAiDeviceSuggestion(name, model) },
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
}
