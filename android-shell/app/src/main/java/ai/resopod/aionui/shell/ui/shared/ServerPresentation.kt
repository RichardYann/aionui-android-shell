package ai.resopod.aionui.shell.ui.shared

import ai.resopod.aionui.shell.data.ServerEntry

data class ServerPresentation(
  val primaryText: String,
  val secondaryText: String?,
  val showFavoriteBadge: Boolean,
  val showRecentBadge: Boolean,
) {
  companion object {
    fun from(server: ServerEntry, currentUrl: String?): ServerPresentation =
      ServerPresentation(
        primaryText = server.primaryLabel(),
        secondaryText = if (server.hasDisplayName()) server.url else null,
        showFavoriteBadge = server.isFavorite,
        showRecentBadge = server.matchesUrl(currentUrl.orEmpty()),
      )
  }
}
