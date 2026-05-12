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
import ai.resopod.aionui.shell.R
import ai.resopod.aionui.shell.data.AppPrefs
import ai.resopod.aionui.shell.data.ServerEntry
import ai.resopod.aionui.shell.ui.web.WebActivity
import ai.resopod.aionui.shell.util.UrlUtil

class ConnectActivity : AppCompatActivity() {
  private lateinit var prefs: AppPrefs
  private lateinit var nameInput: EditText
  private lateinit var urlInput: EditText
  private lateinit var savedServersTitle: TextView
  private lateinit var serverListContainer: LinearLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_connect)

    prefs = AppPrefs(this)
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

  private fun connectToInput() {
    val normalized = UrlUtil.normalize(urlInput.text?.toString() ?: "")
    if (normalized.isBlank()) return

    prefs.upsertServer(nameInput.text?.toString().orEmpty(), normalized, markUsed = true)
    startActivity(Intent(this, WebActivity::class.java))
    finish()
  }

  private fun renderServerList() {
    serverListContainer.removeAllViews()
    val servers = prefs.getServers()
    savedServersTitle.visibility = if (servers.isEmpty()) View.GONE else View.VISIBLE

    if (servers.isEmpty()) {
      serverListContainer.addView(
        TextView(this).apply {
          text = getString(R.string.connect_saved_servers_empty)
          alpha = 0.72f
        },
      )
      return
    }

    servers.forEach { server ->
      val row =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(24, 20, 24, 20)
          background = getDrawable(android.R.drawable.dialog_holo_light_frame)
          setOnClickListener { connectToServer(server) }
          setOnLongClickListener {
            showServerActions(server)
            true
          }
        }

      val title =
        TextView(this).apply {
          text =
            if (server.isFavorite) {
              getString(R.string.server_favorite_prefix, server.primaryLabel())
            } else {
              server.primaryLabel()
            }
          textSize = 16f
          setTypeface(typeface, Typeface.BOLD)
        }

      val subtitle =
        TextView(this).apply {
          text = server.url
        }

      row.addView(title)
      row.addView(subtitle)

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

  private fun connectToServer(server: ServerEntry) {
    prefs.touchServer(server.id)
    startActivity(Intent(this, WebActivity::class.java))
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
}
