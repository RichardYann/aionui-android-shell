package ai.resopod.aionui.shell.data

data class ServerEntry(
  val id: String,
  val displayName: String,
  val url: String,
  val isFavorite: Boolean,
  val lastUsedAt: Long,
) {
  fun primaryLabel(): String = displayName.ifBlank { url }

  fun hasDisplayName(): Boolean = displayName.isNotBlank()

  fun matchesUrl(otherUrl: String): Boolean = url == otherUrl
}
