package com.monorama.airmonomatekr.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.data.model.Project
import com.monorama.airmonomatekr.data.model.TransmissionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDeviceDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (Long, TransmissionMode, Int) -> Unit
) {
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var transmissionMode by remember { mutableStateOf(TransmissionMode.REALTIME) }
    var uploadInterval by remember { mutableStateOf(5) }

    println("RegisterDeviceDialog: projects = $projects")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Device") },
        text = {
            Column {
                // 프로젝트 선택 드롭다운
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProject?.projectName ?: "Select Project",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(), // 드롭다운 위치를 제대로 설정
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.projectName) },
                                onClick = {
                                    selectedProject = project
                                    expanded = false
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                // 전송 모드 선택
                Text("Transmission Mode", style = MaterialTheme.typography.titleMedium)
                TransmissionMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (transmissionMode == mode),
                                onClick = { transmissionMode = mode }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (transmissionMode == mode),
                            onClick = null // `Row`의 `onClick`에서 처리하므로 null
                        )
                        Text(
                            text = when (mode) {
                                TransmissionMode.REALTIME -> "Real-time"
                                TransmissionMode.MINUTE -> "Minute"
                                TransmissionMode.DAILY -> "Daily"
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 업로드 간격 입력
                OutlinedTextField(
                    value = uploadInterval.toString(),
                    onValueChange = { uploadInterval = it.toIntOrNull() ?: 3 },
                    label = { Text("Upload Interval (minutes)") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProject?.let {
                        onConfirm(it.projectId, transmissionMode, uploadInterval)
                    }
                }
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}