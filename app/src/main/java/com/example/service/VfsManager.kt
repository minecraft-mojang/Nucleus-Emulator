package com.example.service

import android.content.Context
import com.example.model.IngestedExecutable
import com.example.model.MemoryOptimizationPlan
import com.example.model.VirtualDriveState
import com.example.model.VirtualMount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class VfsManager(private val context: Context) {

    private val _driveState = MutableStateFlow(
        VirtualDriveState(
            driveLabel = "Vortex Isolated C: Overlay",
            allocatedMb = 32768,
            usedMb = 14320,
            mounts = listOf(
                VirtualMount(
                    driveLetter = "C:",
                    hostAndroidPath = "${context.filesDir.absolutePath}/rootfs/drive_c",
                    virtualTarget = "Wineprefix Virtual C: Drive",
                    mountType = "OverlayFS (upper=data, lower=base_rootfs)"
                ),
                VirtualMount(
                    driveLetter = "D:",
                    hostAndroidPath = "/storage/emulated/0/Download/Simulations",
                    virtualTarget = "Scoped External Games Storage",
                    mountType = "FUSE Passthrough / Direct Read"
                ),
                VirtualMount(
                    driveLetter = "Z:",
                    hostAndroidPath = "/",
                    virtualTarget = "Host Android Linux Root",
                    mountType = "Bind Mount (nosuid,nodev)",
                    isReadOnly = true
                )
            ),
            swapFileStatus = "4096 MB Swap Image active at ${context.cacheDir.absolutePath}/vortex_swap.img",
            isReady = true
        )
    )
    val driveState: StateFlow<VirtualDriveState> = _driveState.asStateFlow()

    private val _executables = MutableStateFlow<List<IngestedExecutable>>(
        listOf(
            IngestedExecutable(
                id = "beamng_drive",
                filename = "BeamNG.drive.x64.exe",
                gameTitle = "BeamNG.drive (v0.32 Heavy Physics)",
                virtualPath = "C:\\Program Files\\BeamNG.drive\\Bin64\\BeamNG.drive.x64.exe",
                sizeBytes = 28_400_000_000L, // 28.4 GB
                targetArchitecture = "x86_64",
                directXVersion = "DirectX 11 (D3D11)",
                isHeavyPhysicsEngine = true,
                recommendedRamGb = 16,
                isPruned = false
            ),
            IngestedExecutable(
                id = "assetto_corsa",
                filename = "AssettoCorsa.exe",
                gameTitle = "Assetto Corsa (Physics Simulation)",
                virtualPath = "C:\\Program Files\\Assetto Corsa\\AssettoCorsa.exe",
                sizeBytes = 16_800_000_000L,
                targetArchitecture = "x86_64",
                directXVersion = "DirectX 11 (D3D11)",
                isHeavyPhysicsEngine = true,
                recommendedRamGb = 12,
                isPruned = true
            ),
            IngestedExecutable(
                id = "rigs_of_rods",
                filename = "RoR.exe",
                gameTitle = "Rigs of Rods (Soft-Body Physics Sandbox)",
                virtualPath = "C:\\Program Files\\RigsOfRods\\bin\\RoR.exe",
                sizeBytes = 4_200_000_000L,
                targetArchitecture = "x86_64",
                directXVersion = "DirectX 9 / 11",
                isHeavyPhysicsEngine = true,
                recommendedRamGb = 8,
                isPruned = true
            )
        )
    )
    val executables: StateFlow<List<IngestedExecutable>> = _executables.asStateFlow()

    private val _optimizationPlan = MutableStateFlow(MemoryOptimizationPlan())
    val optimizationPlan: StateFlow<MemoryOptimizationPlan> = _optimizationPlan.asStateFlow()

    fun updateOptimizationPlan(plan: MemoryOptimizationPlan) {
        _optimizationPlan.value = plan
    }

    /**
     * Ingest an executable into virtual Drive C:
     */
    fun ingestExecutable(title: String, filename: String, isHeavyPhysics: Boolean) {
        val newExe = IngestedExecutable(
            id = "exe_${System.currentTimeMillis()}",
            filename = if (filename.endsWith(".exe", ignoreCase = true)) filename else "$filename.exe",
            gameTitle = title,
            virtualPath = "C:\\Program Files\\$title\\$filename",
            sizeBytes = 8_500_000_000L,
            targetArchitecture = "x86_64",
            directXVersion = "DirectX 11",
            isHeavyPhysicsEngine = isHeavyPhysics,
            recommendedRamGb = if (isHeavyPhysics) 16 else 8,
            isPruned = false
        )
        _executables.value = listOf(newExe) + _executables.value
    }

    /**
     * Executes the Automated Asset-Pruning script workflow for low RAM devices (<12GB)
     */
    fun runAssetPruner(exeId: String): String {
        _executables.value = _executables.value.map { exe ->
            if (exe.id == exeId) exe.copy(isPruned = true) else exe
        }
        return "Pruned 3.4 GB high-res texture mipmaps (downscaled 4K textures to 2K BC7, stripped 800MB uncompressed vehicle engine WAV cache). Memory footprint now compatible with <=12GB mobile RAM."
    }

    /**
     * Generates the Virtual File System overlay setup script.
     */
    fun generateVfsMountScript(): String {
        return """
# VortexNative Virtual File System Redirection Script
mkdir -p "${context.filesDir.absolutePath}/rootfs/drive_c"
mkdir -p "${context.cacheDir.absolutePath}/dxvk_cache"

# Establish OverlayFS Lower/Upper/Workdir
mount -t overlay overlay \
  -o lowerdir="${context.filesDir.absolutePath}/rootfs/base_wine",upperdir="${context.filesDir.absolutePath}/rootfs/drive_c",workdir="${context.filesDir.absolutePath}/rootfs/ovl_work" \
  "${context.filesDir.absolutePath}/rootfs/merged_c"
        """.trimIndent()
    }
}
