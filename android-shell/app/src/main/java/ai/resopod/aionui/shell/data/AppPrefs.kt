package ai.resopod.aionui.shell.data

import android.content.Context

class AppPrefs(context: Context) {
  private val prefs = context.getSharedPreferences("aionui_shell_prefs", Context.MODE_PRIVATE)

  fun getLastUrl(): String? = prefs.getString(KEY_LAST_URL, null)

  fun setLastUrl(url: String) {
    prefs.edit().putString(KEY_LAST_URL, url).apply()
  }

  fun clearLastUrl() {
    prefs.edit().remove(KEY_LAST_URL).apply()
  }

  private companion object {
    private const val KEY_LAST_URL = "last_server_url"
  }
}
