# AionUi Android Shell Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add immersive hidden navigation, page-aware nav coloring, clearer loading states, and an AionUI launcher icon to the Android WebView shell.

**Architecture:** Keep the current single-activity WebView shell, but extract page-style parsing and navigation visibility decisions into small testable helpers. The WebView screen gains a top-edge gesture trigger, animated navigation container, lightweight top progress bar, centered loading overlay, and launcher resources sourced from AionUI branding.

**Tech Stack:** Kotlin, Android Views/XML, WebView, JUnit4, Android resource system, GitHub-hosted AionUI logo assets

---

## File Structure

- Modify: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/WebActivity.kt`
  - Wire together gesture reveal, load state handling, WebView callbacks, and color application
- Create: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/PageStyleUtil.kt`
  - Parse CSS color strings and choose readable foreground colors
- Create: `android-shell/app/src/test/java/ai/resopod/aionui/shell/ui/web/PageStyleUtilTest.kt`
  - Unit-test CSS parsing and contrast decisions
- Create: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/NavVisibilityController.kt`
  - Encapsulate reveal/auto-hide timing decisions in pure Kotlin
- Create: `android-shell/app/src/test/java/ai/resopod/aionui/shell/ui/web/NavVisibilityControllerTest.kt`
  - Unit-test reveal and auto-hide eligibility rules
- Modify: `android-shell/app/src/main/res/layout/activity_web.xml`
  - Add top trigger, animated nav container, top progress, and centered loading overlay
- Modify: `android-shell/app/src/main/res/values/strings.xml`
  - Add any content descriptions and loading labels needed by the new UI
- Modify: `android-shell/app/src/main/AndroidManifest.xml`
  - Replace default app icon references
- Create or modify: `android-shell/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create or modify: `android-shell/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create or modify: `android-shell/app/src/main/res/mipmap-*/ic_launcher.png`
- Create or modify: `android-shell/app/src/main/res/mipmap-*/ic_launcher_round.png`
  - Launcher icon resources generated from the selected AionUI logo
- Optional helper asset: `android-shell/app/src/main/res/drawable/aionui_launcher_foreground.xml` or `.png`
  - Stable source asset if the upstream logo needs adaptation

### Task 1: Add Page Style Parsing Helpers

**Files:**
- Create: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/PageStyleUtil.kt`
- Test: `android-shell/app/src/test/java/ai/resopod/aionui/shell/ui/web/PageStyleUtilTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package ai.resopod.aionui.shell.ui.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageStyleUtilTest {
  @Test
  fun parsesRgbColor() {
    assertEquals(0xFF112233.toInt(), PageStyleUtil.parseCssColor("rgb(17, 34, 51)"))
  }

  @Test
  fun parsesHexColor() {
    assertEquals(0xFF336699.toInt(), PageStyleUtil.parseCssColor("#336699"))
  }

  @Test
  fun ignoresTransparentColor() {
    assertNull(PageStyleUtil.parseCssColor("transparent"))
  }

  @Test
  fun choosesDarkForegroundForLightBackground() {
    assertEquals(0xFF111111.toInt(), PageStyleUtil.pickForegroundColor(0xFFF3F4F6.toInt()))
  }

  @Test
  fun choosesLightForegroundForDarkBackground() {
    assertEquals(0xFFFFFFFF.toInt(), PageStyleUtil.pickForegroundColor(0xFF101820.toInt()))
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android-shell; .\gradlew.bat testDebugUnitTest --tests "ai.resopod.aionui.shell.ui.web.PageStyleUtilTest"`

Expected: FAIL with unresolved reference errors for `PageStyleUtil`

- [ ] **Step 3: Write the minimal implementation**

```kotlin
package ai.resopod.aionui.shell.ui.web

import android.graphics.Color
import kotlin.math.roundToInt

object PageStyleUtil {
  private const val DARK_FOREGROUND = 0xFF111111.toInt()
  private const val LIGHT_FOREGROUND = 0xFFFFFFFF.toInt()

  fun parseCssColor(raw: String?): Int? {
    val value = raw?.trim()?.lowercase().orEmpty()
    if (value.isBlank() || value == "transparent") return null

    return runCatching {
      when {
        value.startsWith("rgb(") -> {
          val parts = value.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toInt() }
          Color.argb(255, parts[0], parts[1], parts[2])
        }
        value.startsWith("#") -> Color.parseColor(value)
        else -> Color.parseColor(value)
      }
    }.getOrNull()
  }

  fun pickForegroundColor(backgroundColor: Int): Int {
    val darkness =
      1 - (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255
    return if (darkness >= 0.5) LIGHT_FOREGROUND else DARK_FOREGROUND
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android-shell; .\gradlew.bat testDebugUnitTest --tests "ai.resopod.aionui.shell.ui.web.PageStyleUtilTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/PageStyleUtil.kt android-shell/app/src/test/java/ai/resopod/aionui/shell/ui/web/PageStyleUtilTest.kt
git commit -m "test(android-shell): cover page style parsing"
```

### Task 2: Add Navigation Visibility Rules

**Files:**
- Create: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/NavVisibilityController.kt`
- Test: `android-shell/app/src/test/java/ai/resopod/aionui/shell/ui/web/NavVisibilityControllerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package ai.resopod.aionui.shell.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavVisibilityControllerTest {
  @Test
  fun hidesAfterTimeoutWhenIdle() {
    val controller = NavVisibilityController(autoHideDelayMs = 2500)
    controller.onRevealed(nowMs = 1000)
    assertTrue(controller.shouldHide(nowMs = 4000, isUserInteracting = false, isLoading = false, isMenuOpen = false))
  }

  @Test
  fun staysVisibleWhileLoading() {
    val controller = NavVisibilityController(autoHideDelayMs = 2500)
    controller.onRevealed(nowMs = 1000)
    assertFalse(controller.shouldHide(nowMs = 4000, isUserInteracting = false, isLoading = true, isMenuOpen = false))
  }

  @Test
  fun staysVisibleWhileMenuOpen() {
    val controller = NavVisibilityController(autoHideDelayMs = 2500)
    controller.onRevealed(nowMs = 1000)
    assertFalse(controller.shouldHide(nowMs = 4000, isUserInteracting = false, isLoading = false, isMenuOpen = true))
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android-shell; .\gradlew.bat testDebugUnitTest --tests "ai.resopod.aionui.shell.ui.web.NavVisibilityControllerTest"`

Expected: FAIL with unresolved reference errors for `NavVisibilityController`

- [ ] **Step 3: Write the minimal implementation**

```kotlin
package ai.resopod.aionui.shell.ui.web

class NavVisibilityController(
  private val autoHideDelayMs: Long,
) {
  private var revealedAtMs: Long = 0

  fun onRevealed(nowMs: Long) {
    revealedAtMs = nowMs
  }

  fun onInteraction(nowMs: Long) {
    revealedAtMs = nowMs
  }

  fun shouldHide(
    nowMs: Long,
    isUserInteracting: Boolean,
    isLoading: Boolean,
    isMenuOpen: Boolean,
  ): Boolean {
    if (isUserInteracting || isLoading || isMenuOpen) return false
    return nowMs - revealedAtMs >= autoHideDelayMs
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android-shell; .\gradlew.bat testDebugUnitTest --tests "ai.resopod.aionui.shell.ui.web.NavVisibilityControllerTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/NavVisibilityController.kt android-shell/app/src/test/java/ai/resopod/aionui/shell/ui/web/NavVisibilityControllerTest.kt
git commit -m "test(android-shell): add nav visibility rules"
```

### Task 3: Add Hidden Navigation and Loading Layout

**Files:**
- Modify: `android-shell/app/src/main/res/layout/activity_web.xml`
- Modify: `android-shell/app/src/main/res/values/strings.xml`

- [ ] **Step 1: Update layout with trigger, nav container, and loaders**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
  android:layout_width="match_parent"
  android:layout_height="match_parent">

  <WebView
    android:id="@+id/webView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

  <View
    android:id="@+id/navRevealHandle"
    android:layout_width="match_parent"
    android:layout_height="12dp"
    android:layout_gravity="top" />

  <LinearLayout
    android:id="@+id/navContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="top"
    android:orientation="vertical"
    android:translationY="-80dp">

    <ProgressBar
      android:id="@+id/topLoadingBar"
      style="?android:attr/progressBarStyleHorizontal"
      android:layout_width="match_parent"
      android:layout_height="3dp"
      android:indeterminate="true"
      android:visibility="gone" />

    <LinearLayout
      android:id="@+id/navBar"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:gravity="center_vertical"
      android:orientation="horizontal"
      android:padding="8dp">

      <!-- existing buttons stay here -->
    </LinearLayout>
  </LinearLayout>

  <FrameLayout
    android:id="@+id/centerLoadingOverlay"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clickable="false"
    android:focusable="false"
    android:visibility="gone">

    <ProgressBar
      android:id="@+id/centerLoadingSpinner"
      android:layout_width="40dp"
      android:layout_height="40dp"
      android:layout_gravity="center"
      android:contentDescription="@string/web_loading" />
  </FrameLayout>

  <include
    android:id="@+id/errorOverlay"
    layout="@layout/view_error"
    android:visibility="gone" />
</FrameLayout>
```

- [ ] **Step 2: Add strings for loading and content descriptions**

```xml
<string name="web_loading">Loading</string>
<string name="web_back">Back</string>
<string name="web_refresh">Refresh</string>
<string name="web_more">More</string>
```

- [ ] **Step 3: Verify resources compile**

Run: `cd android-shell; .\gradlew.bat :app:processDebugResources`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android-shell/app/src/main/res/layout/activity_web.xml android-shell/app/src/main/res/values/strings.xml
git commit -m "feat(android-shell): add immersive web chrome layout"
```

### Task 4: Implement WebActivity Behavior

**Files:**
- Modify: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/WebActivity.kt`

- [ ] **Step 1: Add view bindings and state fields**

```kotlin
private lateinit var navContainer: android.view.View
private lateinit var navBar: android.view.View
private lateinit var navRevealHandle: android.view.View
private lateinit var topLoadingBar: android.view.View
private lateinit var centerLoadingOverlay: android.view.View
private lateinit var navVisibilityController: NavVisibilityController

private var isNavVisible = false
private var isPageLoading = false
private var isMoreMenuOpen = false
private var lastAppliedNavColor: Int? = null
```

- [ ] **Step 2: Wire reveal gesture from the top handle only**

```kotlin
navRevealHandle.setOnTouchListener { _, event ->
  when (event.actionMasked) {
    MotionEvent.ACTION_DOWN -> true
    MotionEvent.ACTION_MOVE -> {
      if (event.y > navRevealHandle.height * 2) {
        revealNavigation()
      }
      true
    }
    else -> false
  }
}
```

- [ ] **Step 3: Add minimal reveal/hide methods**

```kotlin
private fun revealNavigation() {
  if (isNavVisible) return
  isNavVisible = true
  navVisibilityController.onRevealed(System.currentTimeMillis())
  navContainer.animate().translationY(0f).setDuration(180).start()
}

private fun hideNavigation() {
  if (!isNavVisible) return
  isNavVisible = false
  navContainer.animate().translationY(-navContainer.height.toFloat()).setDuration(180).start()
}
```

- [ ] **Step 4: Apply load state callbacks**

```kotlin
override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
  isPageLoading = true
  topLoadingBar.visibility = View.VISIBLE
  centerLoadingOverlay.visibility = View.VISIBLE
}

override fun onPageFinished(view: WebView, url: String?) {
  isPageLoading = false
  topLoadingBar.visibility = View.GONE
  centerLoadingOverlay.visibility = View.GONE
  hideError()
  requestNavColorSync()
}
```

- [ ] **Step 5: Extract and apply page color**

```kotlin
private fun requestNavColorSync() {
  webView.evaluateJavascript(
    """
      (function() {
        const body = window.getComputedStyle(document.body).backgroundColor;
        const html = window.getComputedStyle(document.documentElement).backgroundColor;
        return body || html || '';
      })();
    """.trimIndent()
  ) { raw ->
    val color = PageStyleUtil.parseCssColor(raw?.trim('"')) ?: 0xFF101820.toInt()
    applyNavColors(color)
  }
}
```

- [ ] **Step 6: Apply readable nav colors**

```kotlin
private fun applyNavColors(backgroundColor: Int) {
  if (lastAppliedNavColor == backgroundColor) return
  lastAppliedNavColor = backgroundColor
  val foreground = PageStyleUtil.pickForegroundColor(backgroundColor)
  navBar.setBackgroundColor(backgroundColor)
  btnChangeServer.setTextColor(foreground)
  btnBack.setColorFilter(foreground)
  btnRefresh.setColorFilter(foreground)
  btnMore.setColorFilter(foreground)
}
```

- [ ] **Step 7: Add periodic auto-hide check**

```kotlin
private val autoHideRunnable = object : Runnable {
  override fun run() {
    if (
      isNavVisible &&
      navVisibilityController.shouldHide(
        nowMs = System.currentTimeMillis(),
        isUserInteracting = false,
        isLoading = isPageLoading,
        isMenuOpen = isMoreMenuOpen,
      )
    ) {
      hideNavigation()
    }
    navContainer.postDelayed(this, 300)
  }
}
```

- [ ] **Step 8: Update menu and refresh actions to keep nav visible while active**

```kotlin
btnRefresh.setOnClickListener {
  navVisibilityController.onInteraction(System.currentTimeMillis())
  topLoadingBar.visibility = View.VISIBLE
  centerLoadingOverlay.visibility = View.VISIBLE
  webView.reload()
}
```

- [ ] **Step 9: Run focused unit tests and resource build**

Run: `cd android-shell; .\gradlew.bat testDebugUnitTest :app:processDebugResources`

Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/WebActivity.kt
git commit -m "feat(android-shell): add immersive nav and loading states"
```

### Task 5: Replace Launcher Icon with AionUI Branding

**Files:**
- Modify: `android-shell/app/src/main/AndroidManifest.xml`
- Create or modify: `android-shell/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create or modify: `android-shell/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create or modify: `android-shell/app/src/main/res/mipmap-*/ic_launcher.png`
- Create or modify: `android-shell/app/src/main/res/mipmap-*/ic_launcher_round.png`
- Optional helper asset: `android-shell/app/src/main/res/drawable/aionui_launcher_foreground.png`

- [ ] **Step 1: Identify the upstream source logo**

Use the upstream `https://github.com/iOfficeAI/AionUi/tree/main/resources` directory and select a logo file that is square-safe and visually legible at launcher sizes.

Expected result: a single chosen source asset path documented in the commit message or PR notes.

- [ ] **Step 2: Generate Android launcher resources from the chosen logo**

```text
Create:
- mipmap-anydpi-v26/ic_launcher.xml
- mipmap-anydpi-v26/ic_launcher_round.xml
- mipmap-mdpi/ic_launcher.png
- mipmap-hdpi/ic_launcher.png
- mipmap-xhdpi/ic_launcher.png
- mipmap-xxhdpi/ic_launcher.png
- mipmap-xxxhdpi/ic_launcher.png
```

If adaptive icons are used, point the XML foreground layer at the generated AionUI foreground asset.

- [ ] **Step 3: Replace manifest icon references**

```xml
<application
  android:allowBackup="true"
  android:icon="@mipmap/ic_launcher"
  android:roundIcon="@mipmap/ic_launcher_round"
  android:label="@string/app_name"
  android:supportsRtl="true"
  android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar"
  android:usesCleartextTraffic="true">
```

- [ ] **Step 4: Verify launcher resources package correctly**

Run: `cd android-shell; .\gradlew.bat :app:assembleDebug`

Expected: BUILD SUCCESSFUL and `app/build/outputs/apk/debug/app-debug.apk` exists

- [ ] **Step 5: Commit**

```bash
git add android-shell/app/src/main/AndroidManifest.xml android-shell/app/src/main/res/mipmap-anydpi-v26 android-shell/app/src/main/res/mipmap-* android-shell/app/src/main/res/drawable
git commit -m "feat(android-shell): add aionui launcher icon"
```

### Task 6: Final Verification and Docs Touch-Up

**Files:**
- Modify: `android-shell/README.md`

- [ ] **Step 1: Document the polished behavior**

```md
## Mobile Shell UX

- Hidden top navigation revealed from the top edge only
- Navigation color adapts to the page background when possible
- Top progress + centered loading spinner during connection and refresh
- AionUI launcher icon replaces the default Android placeholder
```

- [ ] **Step 2: Run final verification commands**

Run:

```powershell
cd android-shell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Expected:

- unit tests PASS
- debug APK builds successfully

- [ ] **Step 3: Manual smoke test**

Acceptance checks:

- Launch app and confirm AionUI icon appears in launcher
- Connect to a server and confirm nav starts hidden
- Pull from the top edge and confirm nav appears
- Scroll within page content and confirm nav does not appear
- Tap refresh and confirm top progress and centered spinner appear
- Open a light page and a dark page and confirm nav foreground stays readable

- [ ] **Step 4: Commit**

```bash
git add android-shell/README.md
git commit -m "docs(android-shell): document polished mobile shell behavior"
```

## Plan Self-Review

- Spec coverage:
  - Hidden top-edge-only navigation: Tasks 2, 3, 4
  - Navigation auto-hide and pause rules: Tasks 2 and 4
  - Page-aware immersive color: Tasks 1 and 4
  - Top progress and centered loading spinner: Tasks 3 and 4
  - AionUI launcher icon: Task 5
  - Regression/manual verification: Task 6
- Placeholder scan:
  - No `TODO`, `TBD`, or deferred implementation markers remain
  - The only external lookup left is choosing the exact upstream AionUI logo file, which is explicitly scoped and testable in Task 5
- Type consistency:
  - `PageStyleUtil` is used consistently for CSS parsing and foreground decisions
  - `NavVisibilityController` is used consistently for reveal timing and hide eligibility
  - `WebActivity` remains the integration point for gesture, loading, and WebView state
