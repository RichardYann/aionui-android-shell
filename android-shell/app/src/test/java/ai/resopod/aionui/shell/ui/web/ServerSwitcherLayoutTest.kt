package ai.resopod.aionui.shell.ui.web

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerSwitcherLayoutTest {
  @Test
  fun capsListHeightToConfiguredScreenRatio() {
    val result = ServerSwitcherLayout.computeListHeight(contentHeight = 1200, screenHeight = 2000)

    assertEquals(900, result)
  }

  @Test
  fun keepsContentHeightWhenBelowLimit() {
    val result = ServerSwitcherLayout.computeListHeight(contentHeight = 420, screenHeight = 2000)

    assertEquals(420, result)
  }
}
