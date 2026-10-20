package com.example.model

data class VirtualMount(
    val driveLetter: String, // "C:", "D:", "Z:"
    val hostAndroidPath: String,
    val virtualTarget: String,
    val mountType: String, // "OverlayFS / FUSE", "bind", "tmpfs"
    val isReadOnly: Boolean = false
)

data class VirtualDriveState(
    val driveLabel: String = "Vortex Virtual C: Drive",
    val allocatedMb: Long = 16384,
    val usedMb: Long = 6840,
    val mounts: List<VirtualMount> = listOf(
        VirtualMount("C:", "/data/data/com.aistudio.vortexnative.simbox/files/rootfs/drive_c", "Isolated Wine Prefix Root", "OverlayFS (ext4 upper)"),
        VirtualMount("D:", "/storage/emulated/0/Download/Games", "External Scoped Game Storage", "SAF / Direct FUSE passthrough"),
        VirtualMount("Z:", "/", "Android Linux Host Root (Read Only)", "Bind Mount (nosuid,nodev)", isReadOnly = true)
    ),
    val swapFileStatus: String = "Allocated 4096 MB Swap Block at /cache/vortex_swap.img",
    val isReady: Boolean = true
)

data class LiveTelemetry(
    val currentFps: Float = 58.4f,
    val targetFps: Int = 60,
    val frameTimeMs: Float = 17.1f,
    val drsCurrentScalePercent: Int = 85,
    val physicsLoadPercent: Int = 74,
    val renderLoadPercent: Int = 62,
    val thermalState: String = "Nominal (39.2°C)",
    val isThermalThrottling: Boolean = false,
    val activeThreads: List<Pair<String, Int>> = listOf(
        "beamng_physics_master" to 7, // Cortex-X prime
        "beamng_physics_sub0" to 6,   // Performance
        "beamng_physics_sub1" to 5,   // Performance
        "dxvk_command_stream" to 4,   // Performance
        "dxvk_worker" to 3,           // Performance
        "wine_server" to 1,           // Efficiency
        "audio_xaudio2" to 0          // Efficiency
    ),
    val frameHistory: List<Float> = listOf(58.2f, 57.9f, 59.1f, 58.4f, 55.2f, 58.9f, 60.0f, 59.4f, 58.1f)
)
