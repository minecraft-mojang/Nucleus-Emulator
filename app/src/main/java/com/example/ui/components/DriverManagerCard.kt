package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriverPackage
import com.example.model.GpuVendor
import com.example.ui.theme.*

@Composable
fun DriverManagerCard(
    drivers: List<DriverPackage>,
    selectedDriver: DriverPackage,
    onSelectDriver: (String) -> Unit,
    onDownloadDriver: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("driver_manager_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DYNAMIC DRIVER OVERLAYS",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${drivers.count { it.isInstalled }} / ${drivers.size} INSTALLED",
                        color = MatrixGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Vortex dynamically binds the active Vulkan driver into the Wine environment via VK_ICD_FILENAMES. Switch or fetch alternative builds to eliminate game-specific shader compilation bugs.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                drivers.forEach { driver ->
                    DriverItemRow(
                        driver = driver,
                        isSelected = driver.id == selectedDriver.id,
                        onSelect = { onSelectDriver(driver.id) },
                        onDownload = { onDownloadDriver(driver.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DriverItemRow(
    driver: DriverPackage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
            .border(
                width = 1.dp,
                color = if (isSelected) CyberCyan.copy(alpha = 0.8f) else BorderDark,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                if (driver.isInstalled) onSelect() else onDownload()
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = if (driver.isInstalled) onSelect else onDownload,
                    colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = driver.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (driver.vendor == GpuVendor.ADRENO) CyberCyan.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (driver.vendor == GpuVendor.ADRENO) "ADRENO" else "MALI",
                                color = if (driver.vendor == GpuVendor.ADRENO) CyberCyan else NeonOrange,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = driver.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (driver.isInstalled) {
                Surface(
                    color = MatrixGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isSelected) "ACTIVE" else "INSTALLED",
                        color = if (isSelected) CyberCyan else MatrixGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            } else {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download driver",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fetch",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
