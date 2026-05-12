package ai.resopod.aionui.shell.ui.connect

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import ai.resopod.aionui.shell.R
import ai.resopod.aionui.shell.data.AppPrefs
import ai.resopod.aionui.shell.data.ServerEntry
import ai.resopod.aionui.shell.ui.web.WebActivity
import ai.resopod.aionui.shell.ui.shared.ServerPresentation
import ai.resopod.aionui.shell.util.UrlUtil

class ConnectActivity : AppCompatActivity() {
  private lateinit var prefs: AppPrefs
  private lateinit var screenMode: ConnectScreenMode
  private lateinit var nameInput: EditText
  private lateinit var urlInput: EditText
  private lateinit var savedServersTitle: TextView
  private lateinit var serverListContainer: LinearLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_connect)

    prefs = AppPrefs(this)
    screenMode = ConnectScreenMode.fromIntentValue(intent?.getStringExtra(EXTRA_LAUNCH_MODE))
    nameInput = findViewById(R.id.nameInput)
    urlInput = findViewById(R.id.urlInput)
    savedServersTitle = findViewById(R.id.savedServersTitle)
    serverListContainer = findViewById(R.id.serverListContainer)
    val connectButton = findViewById<Button>(R.id.connectButton)

    prefs.getLastUrl()?.let(urlInput::setText)
    renderServerList()

    connectButton.setOnClickListener {
      connectToInput()
    }
  }

  override fun onBackPressed() {
    if (screenMode.showsBackButton) {
      finish()
      return
    }
    super.onBackPressed()
  }

  private fun connectToInput() {
    val normalized = UrlUtil.normalize(urlInput.text?.toString() ?: "")
    if (normalized.isBlank()) return

    prefs.upsertServer(nameInput.text?.toString().orEmpty(), normalized, markUsed = true)
    if (screenMode == ConnectScreenMode.MANAGE) {
      setResult(RESULT_OK)
    } else {
      startActivity(Intent(this, WebActivity::class.java))
    }
    finish()
  }

  private fun renderServerList() {
    serverListContainer.removeAllViews()
    val servers = prefs.getServers()
    val currentUrl = prefs.getLastUrl()
    if (servers.isEmpty()) {
      savedServersTitle.visibility = View.VISIBLE
      serverListContainer.addView(createEmptyStateView())
      return
    }

    servers.forEach { server ->
      val presentation = ServerPresentation.from(server, currentUrl)
      val row =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          background = AppCompatResources.getDrawable(this@ConnectActivity, R.drawable.server_card_background)
          setPadding(28, 24, 28, 24)
          setOnClickListener { connectToServer(server) }
          setOnLongClickListener {
            showServerActions(server)
            true
          }
        }

      val titleRow =
        LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
        }

      val title =
        TextView(this).apply {
          text = presentation.primaryText
          layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
          textSize = 16f
          setTextColor(0xFFFFFFFF.toInt())
          setTypeface(typeface, Typeface.BOLD)
        }

      val favoriteBadge =
        TextView(this).apply {
          background = AppCompatResources.getDrawable(this@ConnectActivity, R.drawable.server_badge_favorite)
          text = getString(R.string.server_badge_favorite)
          setTextColor(0xFF04111A.toInt())
          textSize = 11f
          setTypeface(typeface, Typeface.BOLD)
          setPadding(18, 8, 18, 8)
          visibility = if (presentation.showFavoriteBadge) View.VISIBLE else View.GONE
        }

      val subtitle =
        TextView(this).apply {
          text = presentation.secondaryText
          alpha = 0.8f
          setTextColor(0xFFB7D2E8.toInt())
          textSize = 13f
          visibility = if (presentation.secondaryText == null) View.GONE else View.VISIBLE
        }

      val statusRow =
        LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
          visibility = if (presentation.showRecentBadge) View.VISIBLE else View.GONE
        }

      val recentBadge =
        TextView(this).apply {
          background = AppCompatResources.getDrawable(this@ConnectActivity, R.drawable.server_badge_recent)
          text = getString(R.string.server_badge_recent)
          setTextColor(0xFF04111A.toInt())
          textSize = 11f
          setTypeface(typeface, Typeface.BOLD)
          setPadding(18, 8, 18, 8)
        }

      titleRow.addView(title)
      titleRow.addView(favoriteBadge)
      statusRow.addView(recentBadge)

      row.addView(titleRow)
      row.addView(
        subtitle,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
          topMargin = 8
        },
      )
      row.addView(
        statusRow,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
          topMargin = 14
        },
      )

      val params =
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
          bottomMargin = 16
        }
      serverListContainer.addView(row, params)
    }
  }

  private fun createEmptyStateView(): TextView =
    TextView(this).apply {
      background = AppCompatResources.getDrawable(this@ConnectActivity, R.drawable.server_card_background)
      text = getString(R.string.connect_saved_servers_empty)
      setPadding(28, 24, 28, 24)
      setTextColor(0xFFB7D2E8.toInt())
      textSize = 14f
    }

  private fun connectToServer(server: ServerEntry) {
    prefs.touchServer(server.id)
    if (screenMode == ConnectScreenMode.MANAGE) {
      setResult(RESULT_OK)
    } else {
      startActivity(Intent(this, WebActivity::class.java))
    }
    finish()
  }

  private fun showServerActions(server: ServerEntry) {
    val favoriteLabel =
      if (server.isFavorite) getString(R.string.server_unfavorite) else getString(R.string.server_favorite)
    val items =
      arrayOf(
        getString(R.string.server_edit),
        favoriteLabel,
        getString(R.string.server_delete),
      )

    AlertDialog.Builder(this)
      .setTitle(server.primaryLabel())
      .setItems(items) { _, which ->
        when (which) {
          0 -> showEditServerDialog(server)
          1 -> {
            prefs.toggleFavorite(server.id)
            renderServerList()
          }
          2 -> {
            prefs.deleteServer(server.id)
            renderServerList()
          }
        }
      }
      .show()
  }

  private fun showEditServerDialog(server: ServerEntry) {
    val layout =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(48, 24, 48, 0)
      }
    val nameField =
      EditText(this).apply {
        setText(server.displayName)
        hint = getString(R.string.connect_name_hint)
      }
    val urlField =
      EditText(this).apply {
        setText(server.url)
        hint = getString(R.string.connect_url_hint)
      }

    layout.addView(nameField)
    layout.addView(urlField)

    AlertDialog.Builder(this)
      .setTitle(R.string.server_edit)
      .setView(layout)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val normalized = UrlUtil.normalize(urlField.text?.toString().orEmpty())
        if (normalized.isBlank()) return@setPositiveButton

        val updatedName = nameField.text?.toString().orEmpty().trim()
        prefs.updateServer(server.id) {
          it.copy(displayName = updatedName, url = normalized)
        }
        if (prefs.getLastUrl() == normalized) {
          nameInput.setText(updatedName)
          urlInput.setText(normalized)
        }
        renderServerList()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  companion object {
    const val EXTRA_LAUNCH_MODE = "launch_mode"
  }
}
