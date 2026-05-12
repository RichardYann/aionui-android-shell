package ai.resopod.aionui.shell.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerStoreTest {
  @Test
  fun sortsFavoritesBeforeRecentlyUsedServers() {
    val olderFavorite =
      ServerEntry(
        id = "favorite",
        displayName = "Favorite",
        url = "https://favorite.example",
        isFavorite = true,
        lastUsedAt = 100,
      )
    val newerRegular =
      ServerEntry(
        id = "recent",
        displayName = "Recent",
        url = "https://recent.example",
        isFavorite = false,
        lastUsedAt = 200,
      )

    val result = ServerStore.sortServers(listOf(newerRegular, olderFavorite))

    assertEquals(listOf("favorite", "recent"), result.map(ServerEntry::id))
  }

  @Test
  fun keepsExistingDisplayNameWhenReconnectInputNameIsBlank() {
    val existing =
      listOf(
        ServerEntry(
          id = "server-1",
          displayName = "Home Lab",
          url = "https://lab.example",
          isFavorite = false,
          lastUsedAt = 100,
        ),
      )

    val result =
      ServerStore.upsertServer(
        servers = existing,
        currentLastUrl = null,
        displayName = "",
        url = "https://lab.example",
        markUsed = true,
        now = 300,
        idFactory = { "unused" },
      )

    assertEquals("Home Lab", result.updatedServer.displayName)
    assertEquals("https://lab.example", result.lastUrl)
    assertEquals(300, result.updatedServer.lastUsedAt)
  }

  @Test
  fun updatesCurrentLastUrlWhenEditingActiveServerUrl() {
    val existing =
      listOf(
        ServerEntry(
          id = "server-1",
          displayName = "Office",
          url = "https://old.example",
          isFavorite = true,
          lastUsedAt = 100,
        ),
      )

    val result =
      ServerStore.updateServer(
        servers = existing,
        currentLastUrl = "https://old.example",
        id = "server-1",
      ) {
        it.copy(url = "https://new.example")
      }

    assertEquals("https://new.example", result.lastUrl)
    assertEquals("https://new.example", result.servers.single().url)
  }

  @Test
  fun deletingActiveServerFallsBackToMostRecentRemainingServer() {
    val favorite =
      ServerEntry(
        id = "favorite",
        displayName = "Favorite",
        url = "https://favorite.example",
        isFavorite = true,
        lastUsedAt = 50,
      )
    val recent =
      ServerEntry(
        id = "recent",
        displayName = "Recent",
        url = "https://recent.example",
        isFavorite = false,
        lastUsedAt = 300,
      )
    val active =
      ServerEntry(
        id = "active",
        displayName = "Active",
        url = "https://active.example",
        isFavorite = false,
        lastUsedAt = 200,
      )

    val result =
      ServerStore.deleteServer(
        servers = listOf(favorite, recent, active),
        currentLastUrl = "https://active.example",
        id = "active",
      )

    assertEquals("https://recent.example", result.lastUrl)
    assertEquals(listOf("favorite", "recent"), result.servers.map(ServerEntry::id))
  }

  @Test
  fun touchServerMarksServerAsRecentlyUsedAndCurrent() {
    val existing =
      listOf(
        ServerEntry(
          id = "server-1",
          displayName = "Office",
          url = "https://office.example",
          isFavorite = false,
          lastUsedAt = 100,
        ),
      )

    val result =
      ServerStore.touchServer(
        servers = existing,
        currentLastUrl = null,
        id = "server-1",
        now = 900,
      )

    assertEquals("https://office.example", result.lastUrl)
    assertEquals(900, result.servers.single().lastUsedAt)
  }

  @Test
  fun parseServersSkipsEntriesWithoutRequiredIdentityFields() {
    val parsed =
      ServerStore.parseServers(
        """
          [
            {"id":"","displayName":"Broken","url":"https://broken.example","isFavorite":false,"lastUsedAt":1},
            {"id":"ok","displayName":"Good","url":"https://good.example","isFavorite":true,"lastUsedAt":2}
          ]
        """.trimIndent(),
      )

    assertEquals(1, parsed.size)
    assertEquals("ok", parsed.single().id)
  }

  @Test
  fun serializeRoundTripsServerEntries() {
    val servers =
      listOf(
        ServerEntry(
          id = "server-1",
          displayName = "Home",
          url = "https://home.example",
          isFavorite = true,
          lastUsedAt = 123,
        ),
      )

    val encoded = ServerStore.serializeServers(servers)
    val decoded = ServerStore.parseServers(encoded)

    assertEquals(servers, decoded)
  }

  @Test
  fun primaryLabelFallsBackToUrlWhenDisplayNameBlank() {
    val unnamed =
      ServerEntry(
        id = "server-1",
        displayName = "",
        url = "https://demo.example",
        isFavorite = false,
        lastUsedAt = 0,
      )

    assertEquals("https://demo.example", unnamed.primaryLabel())
    assertFalse(unnamed.hasDisplayName())
    assertTrue(unnamed.matchesUrl("https://demo.example"))
  }
}
