package ai.resopod.aionui.shell.util

object UrlUtil {
  fun normalize(input: String): String {
    val raw = input.trim()
    if (raw.isEmpty()) return raw
    val lower = raw.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) return raw
    return "http://$raw"
  }
}
