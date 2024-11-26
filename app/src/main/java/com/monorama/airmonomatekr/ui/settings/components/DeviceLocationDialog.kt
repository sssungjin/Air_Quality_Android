package com.monorama.airmonomatekr.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.monorama.airmonomatekr.data.model.DeviceLocation

@Composable
fun DeviceLocationDialog(
    currentLocation: DeviceLocation,
    onDismiss: () -> Unit,
    onConfirm: (DeviceLocation) -> Unit
) {
    var floorLevel by remember { mutableStateOf(currentLocation.floorLevel.toString()) }
    var locationType by remember { mutableStateOf(currentLocation.locationType) }
    var description by remember { mutableStateOf(currentLocation.description) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Device Location Details",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Floor Level
                OutlinedTextField(
                    value = floorLevel,
                    onValueChange = { floorLevel = it },
                    label = { Text("Floor Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location Type
                OutlinedTextField(
                    value = locationType,
                    onValueChange = { locationType = it },
                    label = { Text("Location Type") },
                    placeholder = { Text("e.g., Office, Meeting Room, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Additional details about the location") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                DeviceLocation(
                                    floorLevel = floorLevel.toIntOrNull() ?: 1,
                                    locationType = locationType,
                                    description = description
                                )
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 