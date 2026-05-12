package ai.resopod.aionui.shell.ui.connect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectScreenModeTest {
  @Test
  fun treatsManageLaunchModeAsReturnable() {
    assertTrue(ConnectScreenMode.fromIntentValue("manage").showsBackButton)
  }

  @Test
  fun defaultsUnknownModeToEntry() {
    val mode = ConnectScreenMode.fromIntentValue("unexpected")

    assertFalse(mode.showsBackButton)
    assertTrue(mode.isEntry)
  }

  @Test
  fun treatsNullModeAsEntry() {
    val mode = ConnectScreenMode.fromIntentValue(null)

    assertFalse(mode.showsBackButton)
    assertTrue(mode.isEntry)
  }
}
