package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MemoryOptimizationPlan
import com.example.ui.theme.*

@Composable
fun AssetPrunerCard(
    plan: MemoryOptimizationPlan,
    totalRamMb: Long,
    onToggleTextureDownscale: (Boolean) -> Unit,
    onToggleAudioStrip: (Boolean) -> Unit,
    onPreallocateSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRestrictedRam = totalRamMb < 12288 // < 12 GB

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_pruner_card"),
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
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MatrixGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AUTOMATED ASSET PRUNER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MatrixGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (isRestrictedRam) NeonOrange.copy(alpha = 0.2f) else MatrixGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isRestrictedRam) "<12GB RAM SAFEGUARD ACTIVE" else "ADEQUATE RAM DETECTED",
                        color = if (isRestrictedRam) NeonOrange else MatrixGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Vehicle simulation engines like BeamNG exhaust mobile LPDDR5 RAM through uncompressed 4K textures and soundbanks. This pipeline automatically optimizes assets prior to Wine execution.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Switch 1: Texture Downscaling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Downscale 4K/8K Textures to 2K BC7", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("Saves ~2.2 GB VRAM; prevents GPU OOM on high mesh counts", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                Switch(
                    checked = plan.downscale4kTexturesTo2k,
                    onCheckedChange = onToggleTextureDownscale,
                    modifier = Modifier.testTag("switch_downscale_textures")
                )
            }

            // Switch 2: Uncompressed Audio Trimming
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strip Uncompressed Engine Soundbanks (.wav)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("Replaces with streaming Opus/OGG fallback; saves ~800 MB RAM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                Switch(
                    checked = plan.stripUncompressedAudioWav,
                    onCheckedChange = onToggleAudioStrip,
                    modifier = Modifier.testTag("switch_strip_audio")
                )
            }

            // Swap Preallocation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Column {
                        Text("Virtual Memory / Swapfile (ZRAM)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("4096 MB block in /cache", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                Button(
                    onClick = onPreallocateSwap,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("allocate_swap_button")
                ) {
                    Text("Preallocate", color = androidx.compose.ui.graphics.Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
