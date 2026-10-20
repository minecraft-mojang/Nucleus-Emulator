package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Box64Settings
import com.example.model.GpuVendor
import com.example.model.GraphicsSettings
import com.example.model.HardwareProfile
import com.example.model.MemoryOptimizationPlan
import com.example.model.VulkanDriverType
import com.example.nativebridge.NativeBridge
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

    /**
     * Toggles between Path A (Qualcomm Snapdragon / Adreno) and Path B (MediaTek / Mali)
     * so user can inspect hardware-specific optimizations on any device.
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
