package com.example.model

enum class PackageType(val displayName: String, val category: String) {
    BOX64_CORE("Box64 ARM64 Dynarec Binary", "CPU Translator"),
    WINE_ROOTFS("Wine 9.x / Proton GE Base RootFS", "Windows Subsystem"),
    DRIVER_TURNIP("Turnip (Mesa Freedreno) Vulkan Driver", "GPU Driver (Adreno)"),
    DRIVER_MALI_VORTEK("Mali Vortek Runtime Wrapper", "GPU Driver (Mali)"),
    DRIVER_MESA_ZINK("Mesa Zink Gallium3D Pipeline", "GPU Driver (Fallback)"),
    DXVK_LIBS("DXVK 2.3+ Direct3D Vulkan Bridges", "Graphics Translation")
}

enum class DownloadStatus {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    VERIFYING_SHA256,
    EXTRACTING,
    CONFIGURING_PERMISSIONS,
    COMPLETED,
    FAILED
}

data class DownloadProgress(
    val packageId: String,
    val packageName: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val progressPercent: Float = 0f,
    val currentStepDescription: String = "Ready",
    val errorMessage: String? = null
)

data class DownloadablePackage(
    val id: String,
    val name: String,
    val version: String,
    val type: PackageType,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val targetExtractRelativePath: String,
    val executableRelativePaths: List<String> = emptyList(),
    val releaseNotes: String = ""
)

data class DriverPackage(
    val id: String,
    val name: String,
    val version: String,
    val vendor: GpuVendor,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val icdRelativePath: String, // e.g. "usr/lib/vulkan/turnip_24.1.0/freedreno_icd.aarch64.json"
    val libRelativePath: String, // e.g. "usr/lib/vulkan/turnip_24.1.0/libvulkan_freedreno.so"
    val recommendedFlags: Map<String, String>,
    val isInstalled: Boolean = false,
    val isSelected: Boolean = false,
    val description: String = ""
)

data class SetupWizardState(
    val isFirstBootRequired: Boolean = true,
    val isSetupInProgress: Boolean = false,
    val currentPackageIndex: Int = 0,
    val totalPackages: Int = 4,
    val overallProgressPercent: Float = 0f,
    val currentProgress: DownloadProgress = DownloadProgress("init", "Initialization"),
    val consoleLogs: List<String> = emptyList(),
    val isSetupComplete: Boolean = false
)
