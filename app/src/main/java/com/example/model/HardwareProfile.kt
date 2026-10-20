package com.example.model

/**
 * Detected GPU Architecture and Vendor
 */
enum class GpuVendor(val displayName: String, val driverPath: String) {
    ADRENO("Qualcomm Adreno", "Path A: Turnip (Mesa) / KGSL Direct"),
    MALI("ARM Mali / Immortalis", "Path B: Mali Wrapper / Mesa Zink / Vortek"),
    POWERVR("Imagination PowerVR", "Path B: Rogue / Vulkan Generic"),
    XCLIPSE("Samsung Xclipse (AMD RDNA)", "Path A: RADV / AMD Vulkan Port"),
    GENERIC("Generic Vulkan Device", "Standard System Vulkan Layer")
}

/**
 * CPU Core Cluster Classification
 */
enum class CoreCluster(val label: String, val colorHex: Long) {
    PRIME("Prime Core (Cortex-X)", 0xFFFF5722),      // High-performance single-thread physics
    PERFORMANCE("Performance (Cortex-A7x)", 0xFF00E5FF), // Rendering & node-beam threads
    EFFICIENCY("Efficiency (Cortex-A5x)", 0xFF76FF03)    // Background I/O, Wine server, audio
}

data class CpuCoreInfo(
    val coreIndex: Int,
    val cluster: CoreCluster,
    val maxFreqMhz: Int,
    val currentUsagePercent: Int = 0,
    val pinnedThread: String? = null
)

data class HardwareProfile(
    val socName: String,
    val gpuVendor: GpuVendor,
    val gpuRenderer: String,
    val vulkanVersion: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val zramTotalMb: Long,
    val zramUsedMb: Long,
    val cpuCores: List<CpuCoreInfo>,
    val isSnapdragonAdreno: Boolean,
    val supportsUmaZeroCopy: Boolean,
    val recommendedPipeline: String
)

data class Box64Settings(
    val dynarecFastNan: Boolean = true,
    val dynarecFastRound: Boolean = true,
    val dynarecBigBlock: Int = 3, // 0..3
    val dynarecStrongMemory: Int = 1, // 0..2
    val dynarecBleedingEdge: Boolean = true,
    val dynarecSafeflags: Int = 1,
    val physicsThreadAffinityMask: String = "0xF0", // Pin to Prime + Performance cores
    val renderThreadAffinityMask: String = "0x0E",  // Pin to remaining Performance cores
    val customBox64Rc: String = ""
)

enum class VulkanDriverType(val title: String, val description: String) {
    TURNIP_MESA("Turnip (Mesa Zink/Freedreno)", "Bypasses Qualcomm driver bugs, supports VK_EXT_transform_feedback for DX11 stream out"),
    QUALCOMM_PROPRIETARY("Qualcomm System Vulkan", "Stock proprietary Vulkan driver; optimal power efficiency"),
    MALI_VORTEK_WRAPPER("Mali Vortek Runtime Wrapper", "Bypasses Mali pipeline compile hangs via async queue translation"),
    MESA_ZINK_GALLIUM("Mesa Zink + Gallium3D", "OpenGL over Vulkan fallback for simulation debug tools"),
    SYSTEM_DEFAULT("System Default Vulkan", "Standard OEM provided Vulkan ICD loader")
}

data class GraphicsSettings(
    val driverType: VulkanDriverType = VulkanDriverType.TURNIP_MESA,
    val dxvkAsyncShader: Boolean = true,
    val dxvkMaxFrameRate: Int = 60,
    val d3d11MaxFeatureLevel: String = "11_1",
    val vkd3dShaderModel: String = "6_0",
    val dynamicResolutionScaling: Boolean = true,
    val drsTargetFps: Int = 45,
    val drsMinScalePercent: Int = 50,
    val zeroCopyTextureStreaming: Boolean = true
)

data class MemoryOptimizationPlan(
    val assetPruningActive: Boolean = true,
    val downscale4kTexturesTo2k: Boolean = true,
    val stripUncompressedAudioWav: Boolean = true,
    val enableAggressiveSwap: Boolean = true,
    val swapFileAllocatedMb: Int = 4096,
    val projectedRamSavedMb: Int = 3200
)

data class IngestedExecutable(
    val id: String,
    val filename: String,
    val gameTitle: String,
    val virtualPath: String,
    val sizeBytes: Long,
    val targetArchitecture: String = "x86_64",
    val directXVersion: String = "DirectX 11 (D3D11)",
    val isHeavyPhysicsEngine: Boolean = true,
    val recommendedRamGb: Int = 16,
    val isPruned: Boolean = false
)
