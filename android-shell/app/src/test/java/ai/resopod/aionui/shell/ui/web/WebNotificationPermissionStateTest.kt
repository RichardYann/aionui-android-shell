package ai.resopod.aionui.shell.ui.web

import org.junit.Assert.assertEquals
import org.junit.Test

class WebNotificationPermissionStateTest {
  @Test
  fun grantsImmediatelyWhenRuntimePermissionNotRequired() {
    val result =
      WebNotificationPermissionState.from(
        requiresRuntimePermission = false,
        androidPermissionGranted = false,
      )

    assertEquals(WebNotificationPermissionState.GRANT_AFTER_PROMPT, result)
  }

  @Test
  fun requestsRuntimePermissionWhenNeededAndNotYetGranted() {
    val result =
      WebNotificationPermissionState.from(
        requiresRuntimePermission = true,
        androidPermissionGranted = false,
      )

    assertEquals(WebNotificationPermissionState.REQUEST_RUNTIME_PERMISSION, result)
  }

  @Test
  fun grantsWhenRuntimePermissionAlreadyGranted() {
    val result =
      WebNotificationPermissionState.from(
        requiresRuntimePermission = true,
        androidPermissionGranted = true,
      )

    assertEquals(WebNotificationPermissionState.GRANT_AFTER_PROMPT, result)
  }
}
