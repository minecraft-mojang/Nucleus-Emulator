package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.model.CpuCoreInfo
import com.example.model.LiveTelemetry
import com.example.ui.theme.*

@Composable
fun TelemetryCockpit(
    telemetry: LiveTelemetry,
    onSimulateCrash: () -> Unit,
    onToggleThermal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("telemetry_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isThermalThrottling) NeonOrange else MatrixGreen)
                    )
                    Text(
                        text = "VORTEX ENGINE TELEMETRY",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                AssistChip(
                    onClick = onToggleThermal,
                    label = {
                        Text(
                            text = telemetry.thermalState,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = if (telemetry.isThermalThrottling) NeonOrange else MatrixGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SurfaceVariantDark,
                        labelColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("thermal_chip")
                )
            }

            // Key Telemetry Gauges (FPS, Frame Time, DRS Scale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "VULKAN FPS",
                    value = String.format("%.1f", telemetry.currentFps),
                    unit = "/ 60 target",
                    color = if (telemetry.currentFps >= 45f) MatrixGreen else NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "FRAME TIME",
                    value = String.format("%.1f", telemetry.frameTimeMs),
                    unit = "ms latency",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "DRS VIEWPORT",
                    value = "${telemetry.drsCurrentScalePercent}%",
                    unit = if (telemetry.drsCurrentScalePercent < 100) "Dynamic Scaled" else "Native 1080p",
                    color = if (telemetry.drsCurrentScalePercent < 80) NeonOrange else ElectricViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            // Load Progress Bars (Physics node-beam vs Render)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoadBar(
                    label = "Node-and-Beam Physics Load (CPU)",
                    percent = telemetry.physicsLoadPercent,
                    color = NeonOrange,
                    subtext = "Pinning: Prime Cortex-X4 & A720 cluster"
                )
                LoadBar(
                    label = "Vulkan Render / DXVK Command Stream",
                    percent = telemetry.renderLoadPercent,
                    color = CyberCyan,
                    subtext = "Pipeline: Turnip Mesa Async SSBO Compiler"
                )
            }

            // Interactive simulation triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSimulateCrash,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_crash_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsScore,
                        contentDescription = "Simulate Crash",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simulate Crash",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onToggleThermal,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberCyan)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("core_balancer_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Balance,
                        contentDescription = "Thermal Balancer",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Core-Balancer",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun LoadBar(
    label: String,
    percent: Int,
    color: Color,
    subtext: String,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(targetValue = percent / 100f, label = "load_anim")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = BorderDark
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun CoreTopologyCluster(
    cores: List<CpuCoreInfo>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CPU CLUSTER AFFINITY MASK",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Box64 Multi-Threading Isolated",
                    style = MaterialTheme.typography.labelSmall,
                    color = MatrixGreen,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Grid of 8 cores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cores.forEach { core ->
                    CoreBox(core = core, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CoreBox(core: CpuCoreInfo, modifier: Modifier = Modifier) {
    val coreColor = Color(core.cluster.colorHex)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, coreColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "C${core.coreIndex}",
                color = coreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${core.currentUsagePercent}%",
                color = TextPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = when (core.cluster.name) {
                    "PRIME" -> "X-Core"
                    "PERFORMANCE" -> "A7xx"
                    else -> "A5xx"
                },
                color = TextMuted,
                fontSize = 8.sp
            )
        }
    }
}
