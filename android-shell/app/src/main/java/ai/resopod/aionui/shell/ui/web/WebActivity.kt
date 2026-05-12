package ai.resopod.aionui.shell.ui.web

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import ai.resopod.aionui.shell.R
import ai.resopod.aionui.shell.data.AppPrefs
import ai.resopod.aionui.shell.ui.connect.ConnectActivity

class WebActivity : AppCompatActivity() {
  private lateinit var btnBack: ImageButton
  private lateinit var btnRefresh: ImageButton
  private lateinit var btnMore: ImageButton
  private lateinit var btnChangeServer: Button
  private lateinit var navBar: View
  private lateinit var navRevealHandle: View
  private lateinit var topLoadingBar: View
  private lateinit var centerLoadingOverlay: View
  private lateinit var webView: WebView
  private lateinit var errorOverlay: android.view.View
  private lateinit var errorMessage: TextView
  private lateinit var prefs: AppPrefs

  private var lastBackPressedAtMs: Long = 0
  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private var navShown = false
  private var navGestureStartY = 0f
  private var isPageLoading = true
  private val navAutoHideHandler = Handler(Looper.getMainLooper())
  private val navAutoHideRunnable = Runnable { hideNavigation() }

  private val filePickerLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val cb = filePathCallback ?: return@registerForActivityResult
      filePathCallback = null

      val uris: Array<Uri>? =
        when {
          result.resultCode != RESULT_OK -> null
          result.data == null -> null
          result.data?.clipData != null -> {
            val clip = result.data?.clipData ?: return@registerForActivityResult
            Array(clip.itemCount) { idx -> clip.getItemAt(idx).uri }
          }
          result.data?.data != null -> arrayOf(result.data!!.data!!)
          else -> null
        }

      cb.onReceiveValue(uris)
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_web)

    prefs = AppPrefs(this)
    val url = prefs.getLastUrl()
    if (url.isNullOrBlank()) {
      startActivity(Intent(this, ConnectActivity::class.java))
      finish()
      return
    }

    navBar = findViewById(R.id.navBar)
    navRevealHandle = findViewById(R.id.navRevealHandle)
    topLoadingBar = findViewById(R.id.topLoadingBar)
    centerLoadingOverlay = findViewById(R.id.centerLoadingOverlay)
    webView = findViewById(R.id.webView)
    errorOverlay = findViewById(R.id.errorOverlay)
    errorMessage = errorOverlay.findViewById(R.id.errorMessage)

    btnBack = findViewById(R.id.btnBack)
    btnRefresh = findViewById(R.id.btnRefresh)
    btnMore = findViewById(R.id.btnMore)
    btnChangeServer = findViewById(R.id.btnChangeServer)

    val btnRetry = errorOverlay.findViewById<Button>(R.id.btnRetry)
    val btnErrorChangeServer = errorOverlay.findViewById<Button>(R.id.btnErrorChangeServer)

    btnBack.setOnClickListener {
      keepNavigationVisible()
      if (webView.canGoBack()) webView.goBack() else finish()
    }
    btnRefresh.setOnClickListener {
      keepNavigationVisible()
      webView.reload()
    }
    btnChangeServer.setOnClickListener {
      keepNavigationVisible()
      goToConnect()
    }
    btnErrorChangeServer.setOnClickListener { goToConnect() }
    btnRetry.setOnClickListener {
      hideError()
      webView.reload()
    }
    btnMore.setOnClickListener {
      keepNavigationVisible()
      showMoreMenu()
    }

    navRevealHandle.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          navGestureStartY = event.y
          true
        }
        MotionEvent.ACTION_MOVE -> {
          if (!navShown && event.y - navGestureStartY > navRevealHandle.height * 1.5f) {
            showNavigation()
          }
          true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
        else -> false
      }
    }

    configureWebView()
    webView.loadUrl(url)
  }

  override fun onDestroy() {
    navAutoHideHandler.removeCallbacks(navAutoHideRunnable)
    filePathCallback?.onReceiveValue(null)
    filePathCallback = null
    if (::webView.isInitialized) {
      webView.apply {
        stopLoading()
        webChromeClient = null
        destroy()
      }
    }
    super.onDestroy()
  }

  override fun onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack()
      return
    }

    val now = System.currentTimeMillis()
    if (now - lastBackPressedAtMs < 2000) {
      super.onBackPressed()
      return
    }

    lastBackPressedAtMs = now
    Toast.makeText(this, getString(R.string.web_exit_hint), Toast.LENGTH_SHORT).show()
  }

  private fun configureWebView() {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

    webView.settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      mediaPlaybackRequiresUserGesture = false
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
      userAgentString = userAgentString
    }

    webView.webViewClient =
      object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
          val uri = request.url
          val scheme = uri.scheme?.lowercase() ?: ""
          if (scheme == "http" || scheme == "https") return false

          return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
          } catch (_: ActivityNotFoundException) {
            true
          }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
          isPageLoading = true
          topLoadingBar.visibility = View.VISIBLE
          centerLoadingOverlay.visibility = View.VISIBLE
          keepNavigationVisible()
        }

        override fun onPageFinished(view: WebView, url: String?) {
          isPageLoading = false
          topLoadingBar.visibility = View.GONE
          centerLoadingOverlay.visibility = View.GONE
          hideError()
          keepNavigationVisible()
          syncNavigationColor()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
          if (request.isForMainFrame) {
            isPageLoading = false
            topLoadingBar.visibility = View.GONE
            centerLoadingOverlay.visibility = View.GONE
            navAutoHideHandler.removeCallbacks(navAutoHideRunnable)
            navBar.translationY = 0f
            navShown = true
            showError(getString(R.string.web_failed_to_load, request.url.toString()))
          }
        }
      }

    webView.webChromeClient =
      object : WebChromeClient() {
        override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?,
        ): Boolean {
          this@WebActivity.filePathCallback?.onReceiveValue(null)
          this@WebActivity.filePathCallback = filePathCallback

          val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
          }

          return try {
            filePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.web_select_file)))
            true
          } catch (_: ActivityNotFoundException) {
            this@WebActivity.filePathCallback?.onReceiveValue(null)
            this@WebActivity.filePathCallback = null
            false
          }
        }
      }

    webView.setDownloadListener(
      DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        try {
          val request = DownloadManager.Request(url.toUri())
          request.setMimeType(mimeType)
          val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
          request.setTitle(filename)
          request.setDescription(url)
          request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
          request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

          val cookies = cookieManager.getCookie(url)
          if (!cookies.isNullOrBlank()) {
            request.addRequestHeader("Cookie", cookies)
          }
          if (!userAgent.isNullOrBlank()) {
            request.addRequestHeader("User-Agent", userAgent)
          }

          val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
          dm.enqueue(request)
          Toast.makeText(this, getString(R.string.web_downloading), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
          Toast.makeText(this, getString(R.string.web_download_failed), Toast.LENGTH_SHORT).show()
        }
      },
    )
  }

  private fun showError(message: String) {
    errorMessage.text = message
    errorOverlay.visibility = android.view.View.VISIBLE
  }

  private fun hideError() {
    errorOverlay.visibility = android.view.View.GONE
  }

  private fun showNavigation() {
    if (navShown) {
      keepNavigationVisible()
      return
    }
    navShown = true
    navBar.animate().translationY(0f).setDuration(180).start()
    scheduleNavigationHide()
  }

  private fun hideNavigation() {
    if (!navShown || errorOverlay.visibility == View.VISIBLE || isPageLoading) return
    navShown = false
    navBar.animate().translationY(-navBar.height.toFloat()).setDuration(180).start()
  }

  private fun keepNavigationVisible() {
    if (!navShown) return
    scheduleNavigationHide()
  }

  private fun scheduleNavigationHide() {
    navAutoHideHandler.removeCallbacks(navAutoHideRunnable)
    navAutoHideHandler.postDelayed(navAutoHideRunnable, 2500)
  }

  private fun syncNavigationColor() {
    webView.evaluateJavascript(
      """
        (function() {
          const bodyStyle = window.getComputedStyle(document.body || document.documentElement);
          const htmlStyle = window.getComputedStyle(document.documentElement);
          return JSON.stringify({
            body: bodyStyle ? bodyStyle.backgroundColor : "",
            html: htmlStyle ? htmlStyle.backgroundColor : ""
          });
        })();
      """.trimIndent(),
    ) { raw ->
      val decoded = raw?.trim()?.removePrefix("\"")?.removeSuffix("\"")?.replace("\\\"", "\"")
      val candidates = listOf(
        decoded?.substringAfter("\"body\":\"", "")?.substringBefore("\""),
        decoded?.substringAfter("\"html\":\"", "")?.substringBefore("\""),
      )
      val background = candidates.firstNotNullOfOrNull(::parseCssColor) ?: Color.parseColor("#101820")
      val foreground = pickForegroundColor(background)
      navBar.setBackgroundColor(background)
      btnChangeServer.setTextColor(foreground)
      btnBack.setColorFilter(foreground)
      btnRefresh.setColorFilter(foreground)
      btnMore.setColorFilter(foreground)
    }
  }

  private fun parseCssColor(raw: String?): Int? {
    val value = raw?.trim()?.lowercase().orEmpty()
    if (value.isBlank() || value == "transparent" || value == "rgba(0, 0, 0, 0)") return null
    return runCatching {
      when {
        value.startsWith("#") -> Color.parseColor(value)
        value.startsWith("rgb(") -> {
          val parts = value.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toInt() }
          Color.rgb(parts[0], parts[1], parts[2])
        }
        value.startsWith("rgba(") -> {
          val parts = value.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim() }
          val alpha = parts[3].toFloat()
          if (alpha <= 0f) return null
          Color.argb((alpha * 255).toInt(), parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
        else -> Color.parseColor(value)
      }
    }.getOrNull()
  }

  private fun pickForegroundColor(background: Int): Int {
    val darkness =
      1 - (0.299 * Color.red(background) + 0.587 * Color.green(background) + 0.114 * Color.blue(background)) / 255
    return if (darkness >= 0.5) Color.WHITE else Color.parseColor("#111111")
  }

  private fun goToConnect() {
    startActivity(Intent(this, ConnectActivity::class.java))
    finish()
  }

  private fun showMoreMenu() {
    val currentUrl = webView.url ?: prefs.getLastUrl().orEmpty()
    val items = arrayOf(getString(R.string.web_copy_url), getString(R.string.web_open_in_browser))
    AlertDialog.Builder(this)
      .setItems(items) { _, which ->
        when (which) {
          0 -> copyToClipboard(currentUrl)
          1 -> openInBrowser(currentUrl)
        }
      }
      .show()
  }

  private fun copyToClipboard(text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("url", text))
    Toast.makeText(this, getString(R.string.web_copied), Toast.LENGTH_SHORT).show()
  }

  private fun openInBrowser(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {}
  }
}
