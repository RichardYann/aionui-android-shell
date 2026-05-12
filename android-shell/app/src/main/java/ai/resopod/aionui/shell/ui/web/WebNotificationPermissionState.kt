package ai.resopod.aionui.shell.ui.web

enum class WebNotificationPermissionState {
  GRANT_AFTER_PROMPT,
  REQUEST_RUNTIME_PERMISSION,
  ;

  companion object {
    fun from(
      requiresRuntimePermission: Boolean,
      androidPermissionGranted: Boolean,
    ): WebNotificationPermissionState =
      when {
        !requiresRuntimePermission -> GRANT_AFTER_PROMPT
        androidPermissionGranted -> GRANT_AFTER_PROMPT
        else -> REQUEST_RUNTIME_PERMISSION
      }
  }
}
