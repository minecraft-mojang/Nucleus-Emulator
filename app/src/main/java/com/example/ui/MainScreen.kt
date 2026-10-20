package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpuVendor
import com.example.model.IngestedExecutable
import com.example.model.VulkanDriverType
import com.example.nativebridge.NativeCodeDefinitions
import com.example.ui.components.*
import com.example.ui.theme.*

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TELEMETRY("Dashboard", Icons.Default.Speed),
    STORAGE("Virtual C:", Icons.Default.FolderSpecial),
    CONFIG("Tuning", Icons.Default.Tune),
    BLUEPRINTS("Architecture", Icons.Default.Terminal)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.TELEMETRY) }
    var showIngestionDialog by remember { mutableStateOf(false) }

    val hardware by viewModel.hardwareProfile.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val executables by viewModel.executables.collectAsState()
    val driveState by viewModel.driveState.collectAsState()
    val box64Settings by viewModel.box64Settings.collectAsState()
    val graphicsSettings by viewModel.graphicsSettings.collectAsState()
    val memoryPlan by viewModel.memoryPlan.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (showIngestionDialog) {
        GameIngestionDialog(
            onDismiss = { showIngestionDialog = false },
            onIngest = { title, filename, isHeavy ->
                viewModel.ingestGame(title, filename, isHeavy)
                showIngestionDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "VORTEXNATIVE",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Box64 + DXVK Container Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Quick Switcher: Snapdragon Path A vs Mali Path B
                    FilterChip(
                        selected = hardware.isSnapdragonAdreno,
                        onClick = { viewModel.switchHardwareProfile(!hardware.isSnapdragonAdreno) },
                        label = {
                            Text(
                                text = if (hardware.isSnapdragonAdreno) "Snapdragon (Turnip)" else "Mali (Vortek)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (hardware.isSnapdragonAdreno) CyberCyan else NeonOrange)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceVariantDark,
                            labelColor = TextPrimary,
                            selectedContainerColor = SurfaceVariantDark
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("hardware_path_switcher_chip")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianDark)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = SurfaceVariantDark,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.TELEMETRY -> TelemetryTab(
                    hardware = hardware,
                    telemetry = telemetry,
                    executables = executables,
                    onSimulateCrash = { viewModel.triggerCrashSimulation() },
                    onToggleThermal = { viewModel.toggleThermalBalancerStress() },
                    onPruneGame = { viewModel.pruneGameAssets(it) },
                    onOpenIngest = { showIngestionDialog = true }
                )
                AppTab.STORAGE -> StorageTab(
                    driveState = driveState,
                    executables = executables,
                    memoryPlan = memoryPlan,
                    totalRamMb = hardware.totalRamMb,
                    onOpenIngest = { showIngestionDialog = true },
                    onPrune = { viewModel.pruneGameAssets(it) },
                    onToggleTextureDownscale = { viewModel.updateMemoryPlan(memoryPlan.copy(downscale4kTexturesTo2k = it)) },
                    onToggleAudioStrip = { viewModel.updateMemoryPlan(memoryPlan.copy(stripUncompressedAudioWav = it)) },
                    onPreallocateSwap = { viewModel.preallocateSwap() }
                )
                AppTab.CONFIG -> ConfigTab(
                    box64Settings = box64Settings,
                    graphicsSettings = graphicsSettings,
                    isSnapdragon = hardware.isSnapdragonAdreno,
                    onUpdateBox64 = { viewModel.updateBox64Settings(it) },
                    onUpdateGraphics = { viewModel.updateGraphicsSettings(it) }
                )
                AppTab.BLUEPRINTS -> BlueprintsTab(hardware = hardware)
            }
        }
    }
}

@Composable
fun TelemetryTab(
    hardware: com.example.model.HardwareProfile,
    telemetry: com.example.model.LiveTelemetry,
    executables: List<IngestedExecutable>,
    onSimulateCrash: () -> Unit,
    onToggleThermal: () -> Unit,
    onPruneGame: (String) -> Unit,
    onOpenIngest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Active Hardware Pipeline Banner
        item {
            HardwarePipelineBanner(hardware = hardware)
        }

        // Real-Time Simulation Telemetry Cockpit
        item {
            TelemetryCockpit(
                telemetry = telemetry,
                onSimulateCrash = onSimulateCrash,
                onToggleThermal = onToggleThermal
            )
        }

        // Octa-Core Thread Affinity Topology
        item {
            CoreTopologyCluster(cores = hardware.cpuCores)
        }

        // Executables / Simulations Ready in Virtual C:
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOUNTED SIMULATION ENGINES",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onOpenIngest) {
                    Icon(Icons.Default.Add, contentDescription = "Add game", tint = CyberCyan)
                }
            }
        }

        items(executables) { exe ->
            ExecutableCard(
                executable = exe,
                onPrune = { onPruneGame(exe.id) }
            )
        }
    }
}

@Composable
fun HardwarePipelineBanner(hardware: com.example.model.HardwareProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hardware.socName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (hardware.isSnapdragonAdreno) CyberCyan.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (hardware.isSnapdragonAdreno) "PATH A: ADRENO" else "PATH B: MALI",
                        color = if (hardware.isSnapdragonAdreno) CyberCyan else NeonOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "GPU: ${hardware.gpuRenderer}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "RAM: ${hardware.totalRamMb} MB (${hardware.availableRamMb} MB Avail)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MatrixGreen,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Vulkan: ${hardware.vulkanVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricViolet,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = hardware.recommendedPipeline,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ExecutableCard(
    executable: IngestedExecutable,
    onPrune: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("executable_card_${executable.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = executable.gameTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = executable.virtualPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }

                Surface(
                    color = if (executable.isPruned) MatrixGreen.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (executable.isPruned) "PRUNED (<12GB)" else "FULL ASSETS",
                        color = if (executable.isPruned) MatrixGreen else NeonOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${String.format("%.1f", executable.sizeBytes / 1_000_000_000.0)} GB",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = executable.directXVersion,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!executable.isPruned) {
                    TextButton(
                        onClick = onPrune,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("prune_button_${executable.id}")
                    ) {
                        Text("Prune Mipmaps", color = MatrixGreen, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StorageTab(
    driveState: com.example.model.VirtualDriveState,
    executables: List<IngestedExecutable>,
    memoryPlan: com.example.model.MemoryOptimizationPlan,
    totalRamMb: Long,
    onOpenIngest: () -> Unit,
    onPrune: (String) -> Unit,
    onToggleTextureDownscale: (Boolean) -> Unit,
    onToggleAudioStrip: (Boolean) -> Unit,
    onPreallocateSwap: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Storage overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = driveState.driveLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${driveState.usedMb} MB / ${driveState.allocatedMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    LinearProgressIndicator(
                        progress = { driveState.usedMb / driveState.allocatedMb.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyberCyan,
                        trackColor = BorderDark
                    )

                    Text(
                        text = driveState.swapFileStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MatrixGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Asset Pruner Card
        item {
            AssetPrunerCard(
                plan = memoryPlan,
                totalRamMb = totalRamMb,
                onToggleTextureDownscale = onToggleTextureDownscale,
                onToggleAudioStrip = onToggleAudioStrip,
                onPreallocateSwap = onPreallocateSwap
            )
        }

        // Virtual Mounts Table
        item {
            Text(
                text = "VFS ACTIVE MOUNTS (OVERLAYFS / FUSE)",
                style = MaterialTheme.typography.labelLarge,
                color = CyberCyan,
                fontWeight = FontWeight.Bold
            )
        }

        items(driveState.mounts) { mount ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CyberCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = mount.driveLetter,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mount.virtualTarget,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = mount.hostAndroidPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                        Text(
                            text = mount.mountType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MatrixGreen,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Action button to ingest new .exe
        item {
            Button(
                onClick = onOpenIngest,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ingest_new_game_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mount New Windows .EXE Directory", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfigTab(
    box64Settings: com.example.model.Box64Settings,
    graphicsSettings: com.example.model.GraphicsSettings,
    isSnapdragon: Boolean,
    onUpdateBox64: (com.example.model.Box64Settings) -> Unit,
    onUpdateGraphics: (com.example.model.GraphicsSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Box64 Dynarec Tuning Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BOX64 DYNAREC JIT COMPILER",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )

                    // FastNan & FastRound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FastNaN / FastRound IEEE-754", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Eliminates sign/NaN penalty in soft-body node calculations", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = box64Settings.dynarecFastNan,
                            onCheckedChange = { onUpdateBox64(box64Settings.copy(dynarecFastNan = it, dynarecFastRound = it)) }
                        )
                    }

                    // Strong Memory Model
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strong Memory Model (TSO)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Emulates x86 TSO locks on ARM64; required for BeamNG physics locks", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = box64Settings.dynarecStrongMemory > 0,
                            onCheckedChange = { onUpdateBox64(box64Settings.copy(dynarecStrongMemory = if (it) 1 else 0)) }
                        )
                    }

                    // BigBlock Level
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dynarec BigBlock Level", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Level ${box64Settings.dynarecBigBlock}", style = MaterialTheme.typography.bodyMedium, color = CyberCyan, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = box64Settings.dynarecBigBlock.toFloat(),
                            onValueChange = { onUpdateBox64(box64Settings.copy(dynarecBigBlock = it.toInt())) },
                            valueRange = 0f..3f,
                            steps = 2
                        )
                    }
                }
            }
        }

        // Vulkan & DXVK Graphics Tuning Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VULKAN / DXVK GRAPHICS PIPELINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )

                    // Driver selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Active Vulkan Driver Pipeline", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        VulkanDriverType.values().forEach { driver ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (graphicsSettings.driverType == driver) SurfaceVariantDark else Color.Transparent)
                                    .clickable { onUpdateGraphics(graphicsSettings.copy(driverType = driver)) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = graphicsSettings.driverType == driver,
                                    onClick = { onUpdateGraphics(graphicsSettings.copy(driverType = driver)) },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                )
                                Column {
                                    Text(driver.title, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(driver.description, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Dynamic Resolution Scaling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic Resolution Scaling (DRS)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Scales 3D viewport down to 50% when FPS < 30; UI locks at 1080p", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = graphicsSettings.dynamicResolutionScaling,
                            onCheckedChange = { onUpdateGraphics(graphicsSettings.copy(dynamicResolutionScaling = it)) }
                        )
                    }

                    // DXVK Async Shader Pipeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DXVK Async Pipeline Compilation", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Eliminates crash shader compilation stutter", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = graphicsSettings.dxvkAsyncShader,
                            onCheckedChange = { onUpdateGraphics(graphicsSettings.copy(dxvkAsyncShader = it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlueprintsTab(hardware: com.example.model.HardwareProfile) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "PRODUCTION ARCHITECTURE & PROFILES",
                style = MaterialTheme.typography.labelLarge,
                color = CyberCyan,
                fontWeight = FontWeight.Bold
            )
        }

        // Architecture Diagram
        item {
            ConfigCodeBlock(
                title = "system_architecture_schema.txt",
                code = NativeCodeDefinitions.SYSTEM_ARCHITECTURE_DIAGRAM,
                language = "txt"
            )
        }

        // Box64 RC
        item {
            ConfigCodeBlock(
                title = "box64rc (Tuned for BeamNG Node-and-Beam Physics)",
                code = NativeCodeDefinitions.BOX64_CONFIG,
                language = "ini"
            )
        }

        // DXVK Conf
        item {
            ConfigCodeBlock(
                title = "dxvk.conf (Asynchronous Pipeline & Low Frame Latency)",
                code = NativeCodeDefinitions.DXVK_CONFIG,
                language = "ini"
            )
        }

        // JNI C++ Native Bridge Header
        item {
            ConfigCodeBlock(
                title = "vortex_bridge.h (JNI & Hardware Detection Headers)",
                code = NativeCodeDefinitions.CPP_HEADER,
                language = "cpp"
            )
        }

        // JNI C++ Native Bridge Implementation
        item {
            ConfigCodeBlock(
                title = "vortex_bridge.cpp (GPU Probe & sched_setaffinity Pinning)",
                code = NativeCodeDefinitions.CPP_IMPLEMENTATION,
                language = "cpp"
            )
        }

        // NDK Thermal Core-Balancer Daemon
        item {
            ConfigCodeBlock(
                title = "thermal_balancer.cpp (Mali Octa-Core Thermal Daemon)",
                code = NativeCodeDefinitions.CPP_THERMAL_BALANCER,
                language = "cpp"
            )
        }

        // Runtime Shell Execution Wrapper
        item {
            ConfigCodeBlock(
                title = "launch_vortex.sh (Android RootFS Launcher Script)",
                code = NativeCodeDefinitions.SHELL_LAUNCH_SCRIPT,
                language = "bash"
            )
        }
    }
}
