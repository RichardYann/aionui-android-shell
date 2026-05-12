package ai.resopod.aionui.shell.ui.web

import kotlin.math.min

object ServerSwitcherLayout {
  fun computeListHeight(contentHeight: Int, screenHeight: Int): Int {
    val maxHeight = (screenHeight * 0.45f).toInt()
    return min(contentHeight, maxHeight)
  }
}
