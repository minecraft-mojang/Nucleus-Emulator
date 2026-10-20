package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadStatus
import com.example.model.SetupWizardState
import com.example.ui.theme.*

@Composable
fun SetupWizardScreen(
    state: SetupWizardState,
    onStartSetup: () -> Unit,
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll console logs
    LaunchedEffect(state.consoleLogs.size) {
        if (state.consoleLogs.isNotEmpty()) {
            listState.animateScrollToItem(state.consoleLogs.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(32.dp)
                    )
                }

                Column {
                    Text(
                        text = "VORTEX INITIALIZATION",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "First-Boot Modular Runtime & Driver Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyan,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "VortexNative does not bloat APK installation with multi-gigabyte binaries. On first boot, the engine downloads verified ARM64 Box64 dynarec binaries, a stripped Wine 9.x Proton compatibility rootfs, and optimized Vulkan graphics drivers.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Packages Checklist Overview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_packages_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CORE RUNTIME PACKAGES",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    PackageStepRow(
                        index = 1,
                        name = "Box64 ARM64 Dynarec",
                        size = "~12 MB",
                        isCompleted = state.currentPackageIndex > 0 || state.isSetupComplete,
                        isCurrent = state.isSetupInProgress && state.currentPackageIndex == 0
                    )
                    PackageStepRow(
                        index = 2,
                        name = "Wine 9.x Proton GE Compatibility Layer",
                        size = "~145 MB",
                        isCompleted = state.currentPackageIndex > 1 || state.isSetupComplete,
                        isCurrent = state.isSetupInProgress && state.currentPackageIndex == 1
                    )
                    PackageStepRow(
                        index = 3,
                        name = "Turnip Mesa / Mali Vortek Vulkan Drivers",
                        size = "~18 MB",
                        isCompleted = state.currentPackageIndex > 2 || state.isSetupComplete,
                        isCurrent = state.isSetupInProgress && state.currentPackageIndex == 2
                    )
                    PackageStepRow(
                        index = 4,
                        name = "DXVK 2.3+ D3D11/10/9 Translation Bridges",
                        size = "~14 MB",
                        isCompleted = state.currentPackageIndex > 3 || state.isSetupComplete,
                        isCurrent = state.isSetupInProgress && state.currentPackageIndex == 3
                    )
                }
            }

            // Real-Time Progress Section (when downloading)
            AnimatedVisibility(visible = state.isSetupInProgress || state.isSetupComplete) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_progress_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.currentProgress.packageName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = when (state.currentProgress.status) {
                                    DownloadStatus.COMPLETED -> MatrixGreen.copy(alpha = 0.2f)
                                    DownloadStatus.FAILED -> NeonOrange.copy(alpha = 0.2f)
                                    else -> CyberCyan.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = state.currentProgress.status.name,
                                    color = when (state.currentProgress.status) {
                                        DownloadStatus.COMPLETED -> MatrixGreen
                                        DownloadStatus.FAILED -> NeonOrange
                                        else -> CyberCyan
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val animatedProgress by animateFloatAsState(
                            targetValue = if (state.isSetupComplete) 1f else (state.overallProgressPercent / 100f).coerceIn(0f, 1f),
                            label = "setup_overall_progress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyberCyan,
                            trackColor = BorderDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.currentProgress.currentStepDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            if (state.currentProgress.speedBytesPerSec > 0) {
                                Text(
                                    text = "${state.currentProgress.speedBytesPerSec / (1024 * 1024)} MB/s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MatrixGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Live Log Console
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("setup_console_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM INITIALIZATION LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${state.consoleLogs.size} events",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.consoleLogs.size) { index ->
                            Text(
                                text = state.consoleLogs[index],
                                color = if (state.consoleLogs[index].contains("error", ignoreCase = true)) NeonOrange else TextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (!state.isSetupInProgress && !state.isSetupComplete) {
                Button(
                    onClick = onStartSetup,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_setup_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FETCH & INSTALL RUNTIME STACK",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else if (state.isSetupComplete) {
                Button(
                    onClick = onCompleteSetup,
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_container_button")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH VORTEX CONTAINER",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CyberCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("DOWNLOADING & VERIFYING PACKAGES...", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PackageStepRow(
    index: Int,
    name: String,
    size: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> MatrixGreen
                            isCurrent -> CyberCyan
                            else -> SurfaceVariantDark
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        color = if (isCurrent) Color.Black else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCompleted || isCurrent) TextPrimary else TextMuted,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            text = size,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}
