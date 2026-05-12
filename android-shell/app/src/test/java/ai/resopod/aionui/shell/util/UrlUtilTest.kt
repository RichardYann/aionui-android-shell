package ai.resopod.aionui.shell.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlUtilTest {
  @Test
  fun addsHttpSchemeWhenMissing() {
    assertEquals("http://192.168.1.2:25808", UrlUtil.normalize("192.168.1.2:25808"))
  }

  @Test
  fun trimsWhitespace() {
    assertEquals("https://example.com", UrlUtil.normalize("  https://example.com  "))
  }

  @Test
  fun preservesHttpAndHttps() {
    assertEquals("http://example.com", UrlUtil.normalize("http://example.com"))
    assertEquals("https://example.com", UrlUtil.normalize("https://example.com"))
  }
}
