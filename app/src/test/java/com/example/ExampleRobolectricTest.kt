package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nativebridge.NativeBridge
import com.example.service.VfsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VortexNative", appName)
  }

  @Test
  fun `verify hardware detection and vfs initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nativeBridge = NativeBridge(context)
    val hardware = nativeBridge.detectHardware()
    assertNotNull(hardware)
    assertTrue(hardware.cpuCores.isNotEmpty())

    val vfsManager = VfsManager(context)
    val driveState = vfsManager.driveState.value
    assertTrue(driveState.mounts.isNotEmpty())
    assertEquals("C:", driveState.mounts[0].driveLetter)
  }
}
