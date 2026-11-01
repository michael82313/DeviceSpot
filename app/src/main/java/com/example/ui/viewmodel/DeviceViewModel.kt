package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Device
import com.example.data.model.DeviceLocationLog
import com.example.data.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

sealed interface ScanLookupResult {
    data class Found(val device: Device, val formatName: String = "QR / Barcode") : ScanLookupResult
    data class Unknown(val rawCode: String, val formatName: String = "Barcode") : ScanLookupResult
}

class DeviceViewModel(
    private val repository: DeviceRepository,
    val currentUserId: String,
    userEmail: String = "",
    userDisplayName: String = ""
) : ViewModel() {

    private val _userDisplayName = MutableStateFlow(userDisplayName)
    val userDisplayNameState: StateFlow<String> = _userDisplayName.asStateFlow()
    val userDisplayName: String get() = _userDisplayName.value

    private val _userEmail = MutableStateFlow(userEmail)
    val userEmailState: StateFlow<String> = _userEmail.asStateFlow()
    val userEmail: String get() = _userEmail.value

    private val _isEmailVerified = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDevicesIfEmpty()
        }
    }

    val allDevices: StateFlow<List<Device>> = repository.allDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<DeviceLocationLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val distinctLocations: StateFlow<List<String>> = allDevices.map { devices ->
        devices.flatMap { listOf(it.currentLocation, it.storageLocation) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _selectedLocationFilter = MutableStateFlow<String?>(null)
    val selectedLocationFilter: StateFlow<String?> = _selectedLocationFilter.asStateFlow()

    val filteredDevices: StateFlow<List<Device>> = combine(
        allDevices,
        _searchQuery,
        _selectedCategory,
        _selectedStatus,
        _selectedLocationFilter
    ) { devices, query, category, status, locFilter ->
        devices.filter { device ->
            val matchesQuery = if (query.isBlank()) true else {
                device.name.contains(query, ignoreCase = true) ||
                        device.qrCode.contains(query, ignoreCase = true) ||
                        device.model.contains(query, ignoreCase = true) ||
                        device.currentLocation.contains(query, ignoreCase = true) ||
                        device.storageLocation.contains(query, ignoreCase = true) ||
                        device.assignedTo.contains(query, ignoreCase = true)
            }
            val matchesCategory = category == null || device.category == category
            val matchesStatus = status == null || device.status == status
            val matchesLoc = locFilter == null ||
                    device.currentLocation.equals(locFilter, ignoreCase = true) ||
                    device.storageLocation.equals(locFilter, ignoreCase = true)

            matchesQuery && matchesCategory && matchesStatus && matchesLoc
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _scanLookupResult = MutableStateFlow<ScanLookupResult?>(null)
    val scanLookupResult: StateFlow<ScanLookupResult?> = _scanLookupResult.asStateFlow()

    private val _selectedDeviceForDetail = MutableStateFlow<Device?>(null)
    val selectedDeviceForDetail: StateFlow<Device?> = _selectedDeviceForDetail.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingDevice = MutableStateFlow<Device?>(null)
    val editingDevice: StateFlow<Device?> = _editingDevice.asStateFlow()

    private val _deviceForLocationUpdate = MutableStateFlow<Device?>(null)
    val deviceForLocationUpdate: StateFlow<Device?> = _deviceForLocationUpdate.asStateFlow()

    private val _deviceForQrPreview = MutableStateFlow<Device?>(null)
    val deviceForQrPreview: StateFlow<Device?> = _deviceForQrPreview.asStateFlow()

    private val _prefillQrCode = MutableStateFlow<String?>(null)
    val prefillQrCode: StateFlow<String?> = _prefillQrCode.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onStatusSelect(status: String?) {
        _selectedStatus.value = if (_selectedStatus.value == status) null else status
    }

    fun onLocationFilterSelect(location: String?) {
        _selectedLocationFilter.value = if (_selectedLocationFilter.value == location) null else location
    }

    fun onQrScanned(rawCode: String, formatName: String = "QR / Barcode") {
        val trimmed = rawCode.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val foundDevice = repository.findDeviceByQr(trimmed)
            if (foundDevice != null) {
                _scanLookupResult.value = ScanLookupResult.Found(foundDevice, formatName)
            } else {
                _scanLookupResult.value = ScanLookupResult.Unknown(trimmed, formatName)
            }
        }
    }

    fun clearScanResult() {
        _scanLookupResult.value = null
    }

    fun openDeviceDetail(device: Device) {
        _selectedDeviceForDetail.value = device
    }

    fun closeDeviceDetail() {
        _selectedDeviceForDetail.value = null
    }

    fun openAddDevice(initialQr: String? = null) {
        _editingDevice.value = null
        _prefillQrCode.value = initialQr
        _isAddEditOpen.value = true
    }

    fun openEditDevice(device: Device) {
        _editingDevice.value = device
        _prefillQrCode.value = null
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _editingDevice.value = null
        _prefillQrCode.value = null
    }

    fun openUpdateLocation(device: Device) {
        _deviceForLocationUpdate.value = device
    }

    fun closeUpdateLocation() {
        _deviceForLocationUpdate.value = null
    }

    fun openQrPreview(device: Device) {
        _deviceForQrPreview.value = device
    }

    fun closeQrPreview() {
        _deviceForQrPreview.value = null
    }

    fun saveDevice(device: Device) {
        viewModelScope.launch {
            repository.saveDevice(device)
            closeAddEdit()
            if (_selectedDeviceForDetail.value?.id == device.id) {
                _selectedDeviceForDetail.value = device
            }
        }
    }

    fun updateLocation(
        device: Device,
        newLocation: String,
        newStatus: String,
        actor: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.updateLocation(device, newLocation, newStatus, actor, note)
            closeUpdateLocation()
            clearScanResult()
            if (_selectedDeviceForDetail.value?.id == device.id) {
                _selectedDeviceForDetail.value = device.copy(
                    currentLocation = newLocation.trim(),
                    status = newStatus,
                    assignedTo = if (newStatus == Device.STATUS_STORED) "" else actor.ifBlank { device.assignedTo }
                )
            }
        }
    }

    fun quickReturnToStorage(device: Device) {
        updateLocation(
            device = device,
            newLocation = device.storageLocation,
            newStatus = Device.STATUS_STORED,
            actor = "",
            note = "Returned to default home storage spot"
        )
    }

    fun getLogsForDevice(deviceId: String): Flow<List<DeviceLocationLog>> = repository.getLogsForDevice(deviceId)

    // Gemini AI Assistant Integration
    private val aiAssistant = com.example.ai.DeviceAiAssistant()

    sealed interface AiSuggestionUiState {
        data object Idle : AiSuggestionUiState
        data object Loading : AiSuggestionUiState
        data class Success(val suggestion: com.example.ai.DeviceAiSuggestion) : AiSuggestionUiState
        data class Error(val message: String) : AiSuggestionUiState
    }

    private val _aiSuggestionState = MutableStateFlow<AiSuggestionUiState>(AiSuggestionUiState.Idle)
    val aiSuggestionState: StateFlow<AiSuggestionUiState> = _aiSuggestionState.asStateFlow()

    fun resetAiSuggestion() {
        _aiSuggestionState.value = AiSuggestionUiState.Idle
    }

    fun requestAiDeviceSuggestion(deviceName: String, model: String) {
        if (deviceName.isBlank()) {
            _aiSuggestionState.value = AiSuggestionUiState.Error("Please enter a device name first.")
            return
        }

        viewModelScope.launch {
            _aiSuggestionState.value = AiSuggestionUiState.Loading
            val result = aiAssistant.suggestDeviceDetails(deviceName, model)
            result.fold(
                onSuccess = { suggestion ->
                    _aiSuggestionState.value = AiSuggestionUiState.Success(suggestion)
                },
                onFailure = { error ->
                    _aiSuggestionState.value = AiSuggestionUiState.Error(
                        error.localizedMessage ?: "Gemini recommendation failed."
                    )
                }
            )
        }
    }

    // AI Hardware Locator dialog
    private val _isAiLocatorOpen = MutableStateFlow(false)
    val isAiLocatorOpen: StateFlow<Boolean> = _isAiLocatorOpen.asStateFlow()

    private val _aiLocatorResponse = MutableStateFlow("")
    val aiLocatorResponse: StateFlow<String> = _aiLocatorResponse.asStateFlow()

    private val _isAiLocatorStreaming = MutableStateFlow(false)
    val isAiLocatorStreaming: StateFlow<Boolean> = _isAiLocatorStreaming.asStateFlow()

    private val _aiLocatorError = MutableStateFlow<String?>(null)
    val aiLocatorError: StateFlow<String?> = _aiLocatorError.asStateFlow()

    fun openAiLocator() {
        _isAiLocatorOpen.value = true
        _aiLocatorResponse.value = ""
        _aiLocatorError.value = null
    }

    fun closeAiLocator() {
        _isAiLocatorOpen.value = false
    }

    fun askAiLocator(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiLocatorStreaming.value = true
            _aiLocatorResponse.value = ""
            _aiLocatorError.value = null

            try {
                aiAssistant.askLocatorStream(
                    userQuestion = question,
                    devices = allDevices.value,
                    recentLogs = allLogs.value
                ).collect { chunk ->
                    _aiLocatorResponse.value += chunk
                }
            } catch (e: Exception) {
                _aiLocatorError.value = e.localizedMessage ?: "Failed to ask Gemini."
            } finally {
                _isAiLocatorStreaming.value = false
            }
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            if (_selectedDeviceForDetail.value?.id == device.id) {
                _selectedDeviceForDetail.value = null
            }
        }
    }

    fun updateDisplayName(newName: String, onResult: (Result<Unit>) -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Name cannot be empty")))
            return
        }
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = trimmed
                    }
                    user.updateProfile(profileUpdates).await()
                    user.reload().await()
                    _userDisplayName.value = trimmed
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(IllegalStateException("No active user session")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updatePassword(newPassword: String, onResult: (Result<Unit>) -> Unit) {
        if (newPassword.length < 6) {
            onResult(Result.failure(IllegalArgumentException("Password must be at least 6 characters")))
            return
        }
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.updatePassword(newPassword).await()
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(IllegalStateException("No active user session")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun sendEmailVerification(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.sendEmailVerification().await()
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(IllegalStateException("No active user session")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun reloadUser(onResult: (isVerified: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.reload().await()
                    val verified = user.isEmailVerified
                    _isEmailVerified.value = verified
                    _userDisplayName.value = user.displayName ?: _userDisplayName.value
                    _userEmail.value = user.email ?: _userEmail.value
                    onResult(verified)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(_isEmailVerified.value)
            }
        }
    }

    fun deleteAccount(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Clean up user devices and audit logs
                    repository.deleteAllUserData()
                    // Delete the auth user
                    user.delete().await()
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(IllegalStateException("No active user session")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
