package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Device

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpdateLocationDialog(
    device: Device,
    availableLocations: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (newLocation: String, newStatus: String, actor: String, note: String) -> Unit
) {
    var locationInput by remember { mutableStateOf(device.currentLocation) }
    var statusInput by remember { mutableStateOf(device.status) }
    var actorInput by remember { mutableStateOf(device.assignedTo) }
    var noteInput by remember { mutableStateOf("") }

    val presetLocations = remember(availableLocations, device.storageLocation) {
        val list = mutableListOf<String>()
        if (device.storageLocation.isNotBlank()) list.add(device.storageLocation)
        list.addAll(availableLocations)
        list.addAll(Device.DEFAULT_LOCATIONS)
        list.distinct().take(8)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("update_location_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Location",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_update_location_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Button: "Return to Home Storage"
                if (device.storageLocation.isNotBlank() && device.currentLocation != device.storageLocation) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                locationInput = device.storageLocation
                                statusInput = Device.STATUS_STORED
                                actorInput = ""
                                noteInput = "Returned to default home storage spot"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Quick Action: Return to Home Storage",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = device.storageLocation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // New Location Field
                Text(
                    text = "New Location / Stored At *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Where is it left or stored now?") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_location_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quick pick places:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetLocations.forEach { loc ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (locationInput.equals(loc, ignoreCase = true))
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                locationInput = loc
                                if (loc.equals(device.storageLocation, ignoreCase = true)) {
                                    statusInput = Device.STATUS_STORED
                                }
                            }
                        ) {
                            Text(
                                text = loc,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (locationInput.equals(loc, ignoreCase = true))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Device.ALL_STATUSES.forEach { st ->
                        FilterChip(
                            selected = statusInput == st,
                            onClick = {
                                statusInput = st
                                if (st == Device.STATUS_STORED && locationInput.isBlank()) {
                                    locationInput = device.storageLocation
                                }
                            },
                            label = { Text(st) },
                            modifier = Modifier.testTag("update_status_chip_${st.replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Left by / With Whom
                OutlinedTextField(
                    value = actorInput,
                    onValueChange = { actorInput = it },
                    label = { Text("Moved by / With Person (Optional)") },
                    placeholder = { Text("e.g. Alex, Michael, Self") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_actor_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Movement Note
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note / Why moved? (Optional)") },
                    placeholder = { Text("e.g. Moved for video shoot, left on charger") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_note_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_update_location_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val loc = locationInput.trim().ifBlank { device.currentLocation }
                            onConfirm(loc, statusInput, actorInput.trim(), noteInput.trim())
                        },
                        modifier = Modifier.testTag("confirm_update_location_btn")
                    ) {
                        Text("Log & Update Spot")
                    }
                }
            }
        }
    }
}
