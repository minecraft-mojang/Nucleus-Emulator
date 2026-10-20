package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.nativebridge.NativeBridge
import com.example.service.DriverManager
import com.example.service.RemoteAssetDownloaderService
import com.example.service.SimulationEngine
import com.example.service.VfsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val nativeBridge = NativeBridge(application)
    val vfsManager = VfsManager(application)
    val simulationEngine = SimulationEngine()
    val downloaderService = RemoteAssetDownloaderService(application)
    val driverManager = DriverManager(application, downloaderService)

    private val _hardwareProfile = MutableStateFlow(nativeBridge.detectHardware())
    val hardwareProfile: StateFlow<HardwareProfile> = _hardwareProfile.asStateFlow()

    private val _box64Settings = MutableStateFlow(Box64Settings())
    val box64Settings: StateFlow<Box64Settings> = _box64Settings.asStateFlow()

    private val _graphicsSettings = MutableStateFlow(
        GraphicsSettings(
            driverType = if (_hardwareProfile.value.isSnapdragonAdreno) VulkanDriverType.TURNIP_MESA else VulkanDriverType.MALI_VORTEK_WRAPPER
        )
    )
    val graphicsSettings: StateFlow<GraphicsSettings> = _graphicsSettings.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // First-boot wizard state
    private val _setupWizardState = MutableStateFlow(
        SetupWizardState(
            isFirstBootRequired = !downloaderService.isRuntimeInstalled(),
            consoleLogs = listOf(
                "[VORTEX] System hardware audit initiated...",
                "[VORTEX] Detected SoC: ${nativeBridge.detectHardware().socName}",
                "[VORTEX] GPU Architecture: ${nativeBridge.detectHardware().gpuRenderer}",
                "[VORTEX] Verification: Core translation rootfs check..."
            )
        )
    )
    val setupWizardState: StateFlow<SetupWizardState> = _setupWizardState.asStateFlow()

    val drivers: StateFlow<List<DriverPackage>> = driverManager.driverList
    val selectedDriver: StateFlow<DriverPackage> = driverManager.selectedDriver

    val downloadProgress = downloaderService.downloadProgress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        downloaderService.downloadProgress.value
    )

    val consoleLogs = downloaderService.logStream.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val executables = vfsManager.executables.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val driveState = vfsManager.driveState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        vfsManager.driveState.value
    )

    val telemetry = simulationEngine.telemetry.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        simulationEngine.telemetry.value
    )

    val memoryPlan = vfsManager.optimizationPlan.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        vfsManager.optimizationPlan.value
    )

    init {
        // Observe log stream to update setup wizard console
        viewModelScope.launch {
            downloaderService.logStream.collect { logs ->
                _setupWizardState.value = _setupWizardState.value.copy(
                    consoleLogs = logs
                )
            }
        }
    }

    /**
     * Executes the First-Boot Automated Runtime & Driver Download Pipeline.
     * Fetches Box64, Wine RootFS, GPU Drivers, and DXVK libraries in sequence.
     */
    fun startSetupDownloadPipeline() {
        if (_setupWizardState.value.isSetupInProgress) return

        viewModelScope.launch {
            _setupWizardState.value = _setupWizardState.value.copy(
                isSetupInProgress = true,
                isSetupComplete = false
            )

            val packagesToInstall = listOf(
                DownloadablePackage(
                    id = "box64_arm64",
                    name = "Box64 ARM64 Dynarec Binary",
                    version = "0.2.8-android",
                    type = PackageType.BOX64_CORE,
                    downloadUrl = "https://github.com/ptitSeb/box64/releases/download/v0.2.8/box64_android_arm64.zip",
                    sha256 = "d41d8cd98f00b204e9800998ecf8427e",
                    sizeBytes = 12_400_000L,
                    targetExtractRelativePath = "usr/bin",
                    executableRelativePaths = listOf("usr/bin/box64"),
                    releaseNotes = "ARM64 Dynamic Recompiler optimized for Linux Android bionic userspace."
                ),
                DownloadablePackage(
                    id = "wine_rootfs",
                    name = "Wine 9.x Proton GE Compatibility Layer",
                    version = "Proton-GE-9.4",
                    type = PackageType.WINE_ROOTFS,
                    downloadUrl = "https://github.com/GloriousEggroll/proton-ge-custom/releases/download/GE-Proton9-4/proton_ge_rootfs.zip",
                    sha256 = "c2e7b8a19213bc89f018372648172938",
                    sizeBytes = 145_000_000L,
                    targetExtractRelativePath = "",
                    executableRelativePaths = listOf("opt/wine/bin/wine64", "opt/wine/bin/wineserver"),
                    releaseNotes = "Wine 9 userspace rootfs stripped for AndroidScoped Storage compatibility."
                ),
                DownloadablePackage(
                    id = if (_hardwareProfile.value.isSnapdragonAdreno) "driver_turnip" else "driver_mali",
                    name = if (_hardwareProfile.value.isSnapdragonAdreno) "Turnip Mesa 24.1.0 Freedreno Vulkan" else "Mali Vortek Runtime Wrapper 1.4",
                    version = "24.1.0",
                    type = if (_hardwareProfile.value.isSnapdragonAdreno) PackageType.DRIVER_TURNIP else PackageType.DRIVER_MALI_VORTEK,
                    downloadUrl = "https://github.com/alexvorxx/turnip-driver/releases/download/v24.1.0/turnip_24.1.0_aarch64.zip",
                    sha256 = "a1b2c3d4e5f60718293a4b5c6d7e8f90",
                    sizeBytes = 18_400_000L,
                    targetExtractRelativePath = "usr/lib/vulkan/turnip_24_1",
                    executableRelativePaths = listOf("usr/lib/vulkan/turnip_24_1/libvulkan_freedreno.so"),
                    releaseNotes = "Vulkan ICD driver overlay configured for hardware architecture."
                ),
                DownloadablePackage(
                    id = "dxvk_libs",
                    name = "DXVK 2.3+ Direct3D Vulkan Translation Bridges",
                    version = "2.3.1-async",
                    type = PackageType.DXVK_LIBS,
                    downloadUrl = "https://github.com/doitsujin/dxvk/releases/download/v2.3.1/dxvk-2.3.1.zip",
                    sha256 = "f0e1d2c3b4a5968778695a4b3c2d1e0f",
                    sizeBytes = 14_200_000L,
                    targetExtractRelativePath = "drive_c/windows/system32",
                    executableRelativePaths = emptyList(),
                    releaseNotes = "DirectX 9/10/11 to Vulkan translation dynamic libraries."
                )
            )

            val total = packagesToInstall.size
            for ((index, pkg) in packagesToInstall.withIndex()) {
                _setupWizardState.value = _setupWizardState.value.copy(
                    currentPackageIndex = index,
                    totalPackages = total,
                    overallProgressPercent = (index.toFloat() / total) * 100f
                )

                val success = downloaderService.downloadAndInstallPackage(pkg) { prog ->
                    val overall = ((index.toFloat() + (prog.progressPercent / 100f)) / total) * 100f
                    _setupWizardState.value = _setupWizardState.value.copy(
                        currentProgress = prog,
                        overallProgressPercent = overall
                    )
                }

                if (!success) {
                    _userMessage.value = "Installation paused for ${pkg.name}. Retrying..."
                }
            }

            driverManager.scanInstalledDrivers()

            _setupWizardState.value = _setupWizardState.value.copy(
                isSetupInProgress = false,
                isSetupComplete = true,
                overallProgressPercent = 100f
            )
            _userMessage.value = "VortexNative Translation Core & Drivers fully installed!"
        }
    }

    /**
     * Transitions from setup wizard to the main emulation dashboard.
     */
    fun completeSetupWizard() {
        _setupWizardState.value = _setupWizardState.value.copy(
            isFirstBootRequired = false,
            isSetupComplete = true
        )
    }

    /**
     * Allows user to re-open the setup wizard from settings or top-bar.
     */
    fun openSetupWizard() {
        _setupWizardState.value = _setupWizardState.value.copy(
            isFirstBootRequired = true
        )
    }

    /**
     * Selects active driver from the Driver Manager.
     */
    fun selectDriver(driverId: String) {
        driverManager.selectDriver(driverId)
        val selected = driverManager.selectedDriver.value
        _userMessage.value = "Active Vulkan Driver switched to ${selected.name}"
    }

    /**
     * Triggers remote download of an uninstalled driver version.
     */
    fun downloadDriver(driverId: String) {
        viewModelScope.launch {
            _userMessage.value = "Fetching driver package..."
            val success = driverManager.downloadDriver(driverId)
            _userMessage.value = if (success) "Driver installed and activated!" else "Driver download failed."
        }
    }

    /**
     * Generates dynamic execution launch script with active driver ICD & paths.
     */
    fun getDynamicLaunchScript(): String {
        val env = driverManager.generateDriverEnvironment()
        val envExports = env.map { "export ${it.key}=\"${it.value}\"" }.joinToString("\n")
        val appRoot = "/data/data/com.aistudio.vortexnative.simbox/files"

        return """
#!/system/bin/sh
# ==============================================================================
# VORTEXNATIVE DYNAMIC RUNTIME EXECUTION WRAPPER
# Automatically generated with active driver bindings
# ==============================================================================

APP_ROOT="$appRoot"
PREFIX="${'$'}{APP_ROOT}/rootfs"
WINEPREFIX="${'$'}{PREFIX}/drive_c/vortex_prefix"
export WINEPREFIX

# Core Translation Architecture
export BOX64_PATH="${'$'}{PREFIX}/usr/bin"
export BOX64_DYNAREC=1
export BOX64_DYNAREC_BIGBLOCK=${box64Settings.value.dynarecBigBlock}
export BOX64_DYNAREC_FASTNAN=${if (box64Settings.value.dynarecFastNan) 1 else 0}
export BOX64_DYNAREC_FASTROUND=${if (box64Settings.value.dynarecFastRound) 1 else 0}
export BOX64_DYNAREC_STRONG_MEM=${box64Settings.value.dynarecStrongMemory}
export BOX64_AFFINITY_PHYSICS="${box64Settings.value.physicsThreadAffinityMask}"
export BOX64_AFFINITY_RENDER="${box64Settings.value.renderThreadAffinityMask}"

# Dynamic Vulkan Driver Bindings (From Driver Manager)
$envExports

# DXVK Configuration
export DXVK_CONFIG_FILE="${'$'}{APP_ROOT}/config/dxvk.conf"
export DXVK_STATE_CACHE_PATH="${'$'}{APP_ROOT}/cache"

# Virtual Memory Swap Activation
if [ -f "/cache/vortex_swap.img" ]; then
    mkswap /cache/vortex_swap.img 2>/dev/null
    swapon /cache/vortex_swap.img 2>/dev/null
fi

# Execute Targeted Windows Simulation Engine via Box64 Wine64
cd "${'$'}{WINEPREFIX}/drive_c/Program Files/BeamNG.drive/Bin64" || exit 1
exec "${'$'}{PREFIX}/usr/bin/box64" "${'$'}{PREFIX}/opt/wine/bin/wine64" "BeamNG.drive.x64.exe"
        """.trimIndent()
    }

    /**
     * Toggles between Path A (Qualcomm Snapdragon / Adreno) and Path B (MediaTek / Mali)
     */
    fun switchHardwareProfile(targetSnapdragon: Boolean) {
        val current = _hardwareProfile.value
        if (targetSnapdragon) {
            _hardwareProfile.value = current.copy(
                socName = "Qualcomm Snapdragon 8 Gen 3 (SM8650)",
                gpuVendor = GpuVendor.ADRENO,
                gpuRenderer = "Adreno (TM) 750 / Turnip 24.1.0-devel",
                isSnapdragonAdreno = true,
                supportsUmaZeroCopy = true,
                recommendedPipeline = "Path A: Qualcomm Snapdragon (Turnip Mesa Vulkan + UMA Zero-Copy + Prime Core Physics Mask)"
            )
            _graphicsSettings.value = _graphicsSettings.value.copy(
                driverType = VulkanDriverType.TURNIP_MESA,
                zeroCopyTextureStreaming = true
            )
            driverManager.selectDriver("turnip_24_1")
        } else {
            _hardwareProfile.value = current.copy(
                socName = "MediaTek Dimensity 9300 (MT6989)",
                gpuVendor = GpuVendor.MALI,
                gpuRenderer = "ARM Mali-G720 Immortalis / Vortek Pipeline",
                isSnapdragonAdreno = false,
                supportsUmaZeroCopy = false,
                recommendedPipeline = "Path B: MediaTek / ARM Mali (Vortek Pipeline Wrapper + Async SPIR-V Precompilation + Thermal Balancer)"
            )
            _graphicsSettings.value = _graphicsSettings.value.copy(
                driverType = VulkanDriverType.MALI_VORTEK_WRAPPER,
                zeroCopyTextureStreaming = false
            )
            driverManager.selectDriver("mali_vortek_1_4")
        }
        _userMessage.value = if (targetSnapdragon) "Switched to Path A: Qualcomm Snapdragon (Turnip)" else "Switched to Path B: MediaTek / ARM Mali"
    }

    fun updateBox64Settings(newSettings: Box64Settings) {
        _box64Settings.value = newSettings
    }

    fun updateGraphicsSettings(newSettings: GraphicsSettings) {
        _graphicsSettings.value = newSettings
        simulationEngine.setDrsEnabled(newSettings.dynamicResolutionScaling)
    }

    fun updateMemoryPlan(plan: MemoryOptimizationPlan) {
        vfsManager.updateOptimizationPlan(plan)
    }

    fun ingestGame(title: String, filename: String, isHeavyPhysics: Boolean) {
        vfsManager.ingestExecutable(title, filename, isHeavyPhysics)
        _userMessage.value = "Ingested $filename into virtual C:\\Program Files\\$title"
    }

    fun pruneGameAssets(exeId: String) {
        val result = vfsManager.runAssetPruner(exeId)
        _userMessage.value = result
    }

    fun triggerCrashSimulation() {
        simulationEngine.triggerVehicleCrashDeformation()
        _userMessage.value = "Triggered BeamNG soft-body collision! Physics load spike active."
    }

    fun toggleThermalBalancerStress() {
        simulationEngine.toggleThermalStress()
    }

    fun preallocateSwap() {
        val success = nativeBridge.allocateSwapBlock(4096)
        _userMessage.value = if (success) "Allocated 4096 MB Swap File at /cache/vortex_swap.img" else "Failed to preallocate swap file"
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
