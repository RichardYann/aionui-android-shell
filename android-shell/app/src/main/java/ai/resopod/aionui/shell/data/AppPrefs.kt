package ai.resopod.aionui.shell.data

import android.content.Context
import java.util.UUID

class AppPrefs(context: Context) {
  private val prefs = context.getSharedPreferences("aionui_shell_prefs", Context.MODE_PRIVATE)

  fun getLastUrl(): String? = prefs.getString(KEY_LAST_URL, null)

  fun setLastUrl(url: String) {
    prefs.edit().putString(KEY_LAST_URL, url).apply()
  }

  fun clearLastUrl() {
    prefs.edit().remove(KEY_LAST_URL).apply()
  }

  fun getServers(): List<ServerEntry> = ServerStore.sortServers(readServers())

  fun upsertServer(displayName: String, url: String, markUsed: Boolean = true): ServerEntry {
    val result =
      ServerStore.upsertServer(
        servers = readServers(),
        currentLastUrl = getLastUrl(),
        displayName = displayName,
        url = url,
        markUsed = markUsed,
        now = System.currentTimeMillis(),
        idFactory = { UUID.randomUUID().toString() },
      )
    persist(result)
    return result.updatedServer ?: error("Expected updated server after upsert")
  }

  fun renameServer(id: String, displayName: String) {
    updateServer(id) { it.copy(displayName = displayName) }
  }

  fun updateServer(id: String, transform: (ServerEntry) -> ServerEntry) {
    val result =
      ServerStore.updateServer(
        servers = readServers(),
        currentLastUrl = getLastUrl(),
        id = id,
        transform = transform,
      )
    persist(result)
  }

  fun toggleFavorite(id: String) {
    updateServer(id) { it.copy(isFavorite = !it.isFavorite) }
  }

  fun deleteServer(id: String) {
    persist(ServerStore.deleteServer(readServers(), getLastUrl(), id))
  }

  fun touchServer(id: String) {
    persist(ServerStore.touchServer(readServers(), getLastUrl(), id, System.currentTimeMillis()))
  }

  private fun readServers(): List<ServerEntry> =
    ServerStore.parseServers(
      prefs.getString(KEY_SAVED_SERVERS, null).orEmpty(),
    )

  private fun persist(result: ServerStore.MutationResult) {
    prefs.edit().putString(KEY_SAVED_SERVERS, ServerStore.serializeServers(result.servers)).apply()
    when (val lastUrl = result.lastUrl) {
      null -> clearLastUrl()
      else -> setLastUrl(lastUrl)
    }
  }

  private companion object {
    private const val KEY_LAST_URL = "last_server_url"
    private const val KEY_SAVED_SERVERS = "saved_servers"
  }
}
