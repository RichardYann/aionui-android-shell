package ai.resopod.aionui.shell.ui.shared

import ai.resopod.aionui.shell.data.ServerEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerPresentationTest {
  @Test
  fun usesDisplayNameAsPrimaryAndUrlAsSecondaryWhenNameExists() {
    val server =
      ServerEntry(
        id = "1",
        displayName = "Office",
        url = "https://office.example",
        isFavorite = true,
        lastUsedAt = 100,
      )

    val result = ServerPresentation.from(server, currentUrl = "https://other.example")

    assertEquals("Office", result.primaryText)
    assertEquals("https://office.example", result.secondaryText)
    assertTrue(result.showFavoriteBadge)
    assertFalse(result.showRecentBadge)
  }

  @Test
  fun avoidsDuplicateSecondaryLineWhenDisplayNameMissing() {
    val server =
      ServerEntry(
        id = "1",
        displayName = "",
        url = "https://lab.example",
        isFavorite = false,
        lastUsedAt = 100,
      )

    val result = ServerPresentation.from(server, currentUrl = "https://lab.example")

    assertEquals("https://lab.example", result.primaryText)
    assertEquals(null, result.secondaryText)
    assertFalse(result.showFavoriteBadge)
    assertTrue(result.showRecentBadge)
  }
}
