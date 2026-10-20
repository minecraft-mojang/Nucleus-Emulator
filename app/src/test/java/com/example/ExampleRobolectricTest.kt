package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nativebridge.NativeBridge
import com.example.service.DriverManager
import com.example.service.RemoteAssetDownloaderService
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

  @Test
  fun `verify driver manager and dynamic launch environment`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val downloader = RemoteAssetDownloaderService(context)
    val driverManager = DriverManager(context, downloader)

    val drivers = driverManager.driverList.value
    assertTrue("Driver catalog should contain Turnip and Mali drivers", drivers.isNotEmpty())

    val selected = driverManager.selectedDriver.value
    assertNotNull(selected)

    val env = driverManager.generateDriverEnvironment()
    assertTrue("Environment should export VK_ICD_FILENAMES", env.containsKey("VK_ICD_FILENAMES"))
    assertTrue("Environment should export LD_LIBRARY_PATH", env.containsKey("LD_LIBRARY_PATH"))
  }
}
