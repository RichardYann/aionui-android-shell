package ai.resopod.aionui.shell.data

import org.json.JSONArray
import org.json.JSONObject

object ServerStore {
  data class MutationResult(
    val servers: List<ServerEntry>,
    val lastUrl: String?,
    val updatedServer: ServerEntry? = null,
  )

  fun sortServers(servers: List<ServerEntry>): List<ServerEntry> =
    servers.sortedWith(
      compareByDescending<ServerEntry> { it.isFavorite }
        .thenByDescending { it.lastUsedAt },
    )

  fun parseServers(raw: String): List<ServerEntry> {
    if (raw.isBlank()) return emptyList()

    return runCatching {
      val array = JSONArray(raw)
      buildList {
        for (index in 0 until array.length()) {
          val item = array.getJSONObject(index)
          val id = item.optString("id").trim()
          val url = item.optString("url").trim()
          if (id.isBlank() || url.isBlank()) continue

          add(
            ServerEntry(
              id = id,
              displayName = item.optString("displayName"),
              url = url,
              isFavorite = item.optBoolean("isFavorite"),
              lastUsedAt = item.optLong("lastUsedAt"),
            ),
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun serializeServers(servers: List<ServerEntry>): String =
    JSONArray().apply {
      servers.forEach { server ->
        put(
          JSONObject().apply {
            put("id", server.id)
            put("displayName", server.displayName)
            put("url", server.url)
            put("isFavorite", server.isFavorite)
            put("lastUsedAt", server.lastUsedAt)
          },
        )
      }
    }.toString()

  fun upsertServer(
    servers: List<ServerEntry>,
    currentLastUrl: String?,
    displayName: String,
    url: String,
    markUsed: Boolean,
    now: Long,
    idFactory: () -> String,
  ): MutationResult {
    val current = servers.toMutableList()
    val index = current.indexOfFirst { it.matchesUrl(url) }
    val updated =
      if (index >= 0) {
        current[index].copy(
          displayName = displayName.ifBlank { current[index].displayName },
          lastUsedAt = if (markUsed) now else current[index].lastUsedAt,
        )
      } else {
        ServerEntry(
          id = idFactory(),
          displayName = displayName,
          url = url,
          isFavorite = false,
          lastUsedAt = now,
        )
      }

    if (index >= 0) current[index] = updated else current.add(updated)
    return MutationResult(
      servers = current,
      lastUrl = if (markUsed) updated.url else currentLastUrl,
      updatedServer = updated,
    )
  }

  fun updateServer(
    servers: List<ServerEntry>,
    currentLastUrl: String?,
    id: String,
    transform: (ServerEntry) -> ServerEntry,
  ): MutationResult {
    val current = servers.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index < 0) return MutationResult(servers = servers, lastUrl = currentLastUrl)

    val previousUrl = current[index].url
    val updated = transform(current[index])
    current[index] = updated
    return MutationResult(
      servers = current,
      lastUrl = if (currentLastUrl == previousUrl) updated.url else currentLastUrl,
      updatedServer = updated,
    )
  }

  fun deleteServer(
    servers: List<ServerEntry>,
    currentLastUrl: String?,
    id: String,
  ): MutationResult {
    val target = servers.firstOrNull { it.id == id } ?: return MutationResult(servers, currentLastUrl)
    val remaining = servers.filterNot { it.id == id }
    val nextLastUrl =
      if (currentLastUrl == target.url) {
        remaining.maxByOrNull { it.lastUsedAt }?.url
      } else {
        currentLastUrl
      }

    return MutationResult(servers = remaining, lastUrl = nextLastUrl)
  }

  fun touchServer(
    servers: List<ServerEntry>,
    currentLastUrl: String?,
    id: String,
    now: Long,
  ): MutationResult =
    updateServer(servers, currentLastUrl, id) {
      it.copy(lastUsedAt = now)
    }.let { result ->
      result.copy(lastUrl = result.updatedServer?.url ?: currentLastUrl)
    }
}
