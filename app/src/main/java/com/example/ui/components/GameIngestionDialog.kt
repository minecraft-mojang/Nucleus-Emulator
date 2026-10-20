package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary

@Composable
fun GameIngestionDialog(
    onDismiss: () -> Unit,
    onIngest: (title: String, filename: String, isHeavyPhysics: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("BeamNG.drive (User Mod)") }
    var filename by remember { mutableStateOf("BeamNG.drive.x64.exe") }
    var isHeavyPhysics by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = CyberCyan)
                Text("Mount Windows .EXE to Virtual C:", color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Select or specify Windows x86_64 simulation binary to stage into the isolated wineprefix overlayfs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Simulation / Game Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ingest_title_input")
                )

                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("Executable Filename (.exe)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ingest_filename_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Soft-Body Physics Offloading (BeamNG-Class)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isHeavyPhysics,
                        onCheckedChange = { isHeavyPhysics = it },
                        modifier = Modifier.testTag("heavy_physics_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && filename.isNotBlank()) {
                        onIngest(title, filename, isHeavyPhysics)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier.testTag("confirm_ingest_button")
            ) {
                Text("Stage & Mount", color = androidx.compose.ui.graphics.Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
