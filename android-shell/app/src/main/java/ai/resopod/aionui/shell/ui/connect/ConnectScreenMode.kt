package ai.resopod.aionui.shell.ui.connect

enum class ConnectScreenMode(
  val value: String,
  val showsBackButton: Boolean,
  val isEntry: Boolean,
) {
  ENTRY(
    value = "entry",
    showsBackButton = false,
    isEntry = true,
  ),
  MANAGE(
    value = "manage",
    showsBackButton = true,
    isEntry = false,
  ),
  ;

  companion object {
    fun fromIntentValue(value: String?): ConnectScreenMode =
      entries.firstOrNull { it.value == value } ?: ENTRY
  }
}
