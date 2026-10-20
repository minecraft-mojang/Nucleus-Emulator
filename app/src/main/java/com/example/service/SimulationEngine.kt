package com.example.service

import com.example.model.LiveTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SimulationEngine {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var telemetryJob: Job? = null

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private var isSimulatingCrash = false
    private var thermalStressActive = false
    private var drsEnabled = true

    init {
        startTelemetryLoop()
    }

    fun setDrsEnabled(enabled: Boolean) {
        drsEnabled = enabled
    }

    /**
     * Simulates a high-stress soft-body crash deformation event in BeamNG.drive
     * causing instantaneous node-and-beam physics spike.
     */
    fun triggerVehicleCrashDeformation() {
        isSimulatingCrash = true
        scope.launch {
            delay(5000) // 5 seconds of intensive crash simulation
            isSimulatingCrash = false
        }
    }

    /**
     * Toggles thermal stress to demonstrate the dynamic core-balancer daemon.
     */
    fun toggleThermalStress() {
        thermalStressActive = !thermalStressActive
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            var currentFps = 58.0f
            var drsScale = 100
            val recentFrames = ArrayDeque<Float>(listOf(58f, 59f, 57f, 60f, 58f, 59f, 60f, 58f, 59f))

            while (true) {
                delay(300)

                val physicsLoad: Int
                val renderLoad: Int
                val targetFps = 60

                if (isSimulatingCrash) {
                    physicsLoad = Random.nextInt(88, 99)
                    renderLoad = Random.nextInt(75, 92)
                    // If DRS is disabled, fps tanks to ~22-26 fps
                    val unscaledFps = Random.nextDouble(22.0, 27.0).toFloat()
                    if (drsEnabled) {
                        // DRS kicks in to drop render resolution viewport from 100% -> 60%
                        drsScale = max(50, drsScale - 8)
                        // With DRS, FPS stays above 38-46 fps
                        currentFps = min(60.0f, (unscaledFps * (100f / drsScale.toFloat()) * 0.72f).coerceAtLeast(36f))
                    } else {
                        currentFps = unscaledFps
                    }
                } else {
                    physicsLoad = Random.nextInt(48, 68)
                    renderLoad = Random.nextInt(42, 60)
                    if (drsScale < 100) {
                        drsScale = min(100, drsScale + 4) // Smoothly restore viewport back to 100%
                    }
                    currentFps = (57.0f + Random.nextFloat() * 3.5f).coerceAtMost(60.0f)
                }

                val frameTimeMs = (1000.0f / currentFps.coerceAtLeast(10f))
                recentFrames.addLast(currentFps)
                if (recentFrames.size > 14) recentFrames.removeFirst()

                val tempC = if (thermalStressActive) 45.4f + Random.nextFloat() * 1.5f else 38.6f + Random.nextFloat() * 1.2f
                val isThrottling = tempC >= 44.0f
                val thermalState = if (isThrottling) {
                    "Core-Balancer ACTIVE (%.1f°C) - Throttling Mitigated".format(tempC)
                } else {
                    "Nominal (%.1f°C)".format(tempC)
                }

                _telemetry.value = LiveTelemetry(
                    currentFps = currentFps,
                    targetFps = targetFps,
                    frameTimeMs = frameTimeMs,
                    drsCurrentScalePercent = drsScale,
                    physicsLoadPercent = physicsLoad,
                    renderLoadPercent = renderLoad,
                    thermalState = thermalState,
                    isThermalThrottling = isThrottling,
                    activeThreads = listOf(
                        "beamng_physics_master" to 7, // Cortex-X prime core
                        "beamng_physics_sub0" to 6,   // Performance
                        "beamng_physics_sub1" to 5,   // Performance
                        "dxvk_command_stream" to 4,   // Performance
                        "dxvk_worker" to 3,           // Performance
                        "wine_server" to 1,           // Efficiency
                        "audio_xaudio2" to 0          // Efficiency
                    ),
                    frameHistory = recentFrames.toList()
                )
            }
        }
    }
}
