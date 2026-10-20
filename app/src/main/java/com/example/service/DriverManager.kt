package com.example.service

import android.content.Context
import com.example.model.DownloadablePackage
import com.example.model.DriverPackage
import com.example.model.GpuVendor
import com.example.model.PackageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages downloaded Vulkan drivers, dynamic overlays, and runtime environment generation.
 */
class DriverManager(
    private val context: Context,
    private val downloaderService: RemoteAssetDownloaderService
) {

    private val rootfsDir = File(context.filesDir, "rootfs")
    private val vulkanDriversDir = File(rootfsDir, "usr/lib/vulkan")

    private val availableDrivers = listOf(
        DriverPackage(
            id = "turnip_24_1",
            name = "Turnip Mesa 24.1.0 (Stable)",
            version = "24.1.0-rel",
            vendor = GpuVendor.ADRENO,
            downloadUrl = "https://github.com/alexvorxx/turnip-driver/releases/download/v24.1.0/turnip_24.1.0_aarch64.zip",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sizeBytes = 18_400_000L,
            icdRelativePath = "usr/lib/vulkan/turnip_24_1/freedreno_icd.aarch64.json",
            libRelativePath = "usr/lib/vulkan/turnip_24_1/libvulkan_freedreno.so",
            recommendedFlags = mapOf(
                "TU_DEBUG" to "sysmem,noconform",
                "MESA_VK_WSI_PRESENT_MODE" to "mailbox"
            ),
            isInstalled = true,
            isSelected = true,
            description = "High-stability Open-Source Mesa driver. Native VK_EXT_transform_feedback support for D3D11 vehicle body deformation."
        ),
        DriverPackage(
            id = "turnip_24_2_devel",
            name = "Turnip Mesa 24.2.0 (Bleeding Edge)",
            version = "24.2.0-devel-git",
            vendor = GpuVendor.ADRENO,
            downloadUrl = "https://github.com/alexvorxx/turnip-driver/releases/download/v24.2.0-dev/turnip_24.2.0_dev.zip",
            sha256 = "b5a2c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b866",
            sizeBytes = 21_200_000L,
            icdRelativePath = "usr/lib/vulkan/turnip_24_2/freedreno_icd.aarch64.json",
            libRelativePath = "usr/lib/vulkan/turnip_24_2/libvulkan_freedreno.so",
            recommendedFlags = mapOf(
                "TU_DEBUG" to "sysmem,noconform,flushall",
                "MESA_VK_WSI_PRESENT_MODE" to "immediate"
            ),
            isInstalled = false,
            isSelected = false,
            description = "Experimental Freedreno compiler with aggressive SPIR-V workgroup caching and Adreno 750/830 optimizations."
        ),
        DriverPackage(
            id = "mali_vortek_1_4",
            name = "Mali Vortek Runtime Wrapper 1.4",
            version = "1.4.2",
            vendor = GpuVendor.MALI,
            downloadUrl = "https://github.com/mali-vortek/vortek-wrapper/releases/download/v1.4.2/mali_vortek_1.4.2.zip",
            sha256 = "c6a2c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b877",
            sizeBytes = 12_800_000L,
            icdRelativePath = "usr/lib/vulkan/mali_vortek/mali_wrapper_icd.json",
            libRelativePath = "usr/lib/vulkan/mali_vortek/libvulkan_mali_wrapper.so",
            recommendedFlags = mapOf(
                "VORTEK_ASYNC_PIPELINE" to "1",
                "VORTEK_QUEUE_BYPASS" to "1"
            ),
            isInstalled = false,
            isSelected = false,
            description = "Bypasses ARM Mali driver queue stalls during vehicle crash deformations with async translation worker threads."
        ),
        DriverPackage(
            id = "mesa_zink_24_0",
            name = "Mesa Zink Gallium3D 24.0",
            version = "24.0.5",
            vendor = GpuVendor.MALI,
            downloadUrl = "https://github.com/mesa-zink/zink-android/releases/download/v24.0.5/zink_gallium_24.0.5.zip",
            sha256 = "d7a2c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b888",
            sizeBytes = 24_600_000L,
            icdRelativePath = "usr/lib/vulkan/zink/zink_icd.json",
            libRelativePath = "usr/lib/vulkan/zink/zink_dri.so",
            recommendedFlags = mapOf(
                "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
                "GALLIUM_DRIVER" to "zink"
            ),
            isInstalled = false,
            isSelected = false,
            description = "Universal OpenGL over Vulkan driver fallback. Useful for legacy physics visualization and telemetry debug tools."
        )
    )

    private val _driverList = MutableStateFlow(availableDrivers)
    val driverList: StateFlow<List<DriverPackage>> = _driverList.asStateFlow()

    private val _selectedDriver = MutableStateFlow(availableDrivers[0])
    val selectedDriver: StateFlow<DriverPackage> = _selectedDriver.asStateFlow()

    init {
        scanInstalledDrivers()
    }

    /**
     * Scans the internal rootfs to update installation status for all driver packages.
     */
    fun scanInstalledDrivers() {
        val updated = _driverList.value.map { driver ->
            val icdFile = File(rootfsDir, driver.icdRelativePath)
            val libFile = File(rootfsDir, driver.libRelativePath)
            val installed = icdFile.exists() || libFile.exists()
            driver.copy(isInstalled = installed)
        }
        _driverList.value = updated
    }

    /**
     * Selects an active graphics driver package and updates environment configurations.
     */
    fun selectDriver(driverId: String) {
        val updated = _driverList.value.map { driver ->
            val isTarget = driver.id == driverId
            driver.copy(isSelected = isTarget)
        }
        _driverList.value = updated
        _selectedDriver.value = updated.firstOrNull { it.id == driverId } ?: updated[0]
    }

    /**
     * Converts a DriverPackage to a DownloadablePackage and invokes the downloader service.
     */
    suspend fun downloadDriver(driverId: String, onProgress: ((com.example.model.DownloadProgress) -> Unit)? = null): Boolean {
        val driver = _driverList.value.firstOrNull { it.id == driverId } ?: return false
        val targetExtractDir = File(driver.icdRelativePath).parent ?: "usr/lib/vulkan/${driver.id}"

        val pkg = DownloadablePackage(
            id = driver.id,
            name = driver.name,
            version = driver.version,
            type = if (driver.vendor == GpuVendor.ADRENO) PackageType.DRIVER_TURNIP else PackageType.DRIVER_MALI_VORTEK,
            downloadUrl = driver.downloadUrl,
            sha256 = driver.sha256,
            sizeBytes = driver.sizeBytes,
            targetExtractRelativePath = targetExtractDir,
            executableRelativePaths = listOf(driver.libRelativePath),
            releaseNotes = driver.description
        )

        val success = downloaderService.downloadAndInstallPackage(pkg, onProgress)
        if (success) {
            scanInstalledDrivers()
            selectDriver(driverId)
        }
        return success
    }

    /**
     * Generates the dynamic environment variables for the selected driver to be used in launch_vortex.sh.
     */
    fun generateDriverEnvironment(): Map<String, String> {
        val driver = _selectedDriver.value
        val rootfsPath = rootfsDir.absolutePath
        val icdPath = "$rootfsPath/${driver.icdRelativePath}"
        val driverDir = File(icdPath).parent ?: "$rootfsPath/usr/lib/vulkan"

        val env = mutableMapOf<String, String>()
        env["VK_ICD_FILENAMES"] = icdPath
        env["LD_LIBRARY_PATH"] = "$driverDir:$rootfsPath/usr/lib/x86_64-linux-gnu:$rootfsPath/lib"

        // Inject driver-specific optimization flags
        driver.recommendedFlags.forEach { (key, value) ->
            env[key] = value
        }

        return env
    }
}
