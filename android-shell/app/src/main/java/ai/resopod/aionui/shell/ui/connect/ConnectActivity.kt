package ai.resopod.aionui.shell.ui.connect

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import ai.resopod.aionui.shell.R
import ai.resopod.aionui.shell.data.AppPrefs
import ai.resopod.aionui.shell.ui.web.WebActivity
import ai.resopod.aionui.shell.util.UrlUtil

class ConnectActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_connect)

    val prefs = AppPrefs(this)
    val urlInput = findViewById<EditText>(R.id.urlInput)
    val connectButton = findViewById<Button>(R.id.connectButton)

    prefs.getLastUrl()?.let(urlInput::setText)

    connectButton.setOnClickListener {
      val normalized = UrlUtil.normalize(urlInput.text?.toString() ?: "")
      if (normalized.isBlank()) return@setOnClickListener
      prefs.setLastUrl(normalized)
      startActivity(Intent(this, WebActivity::class.java))
      finish()
    }
  }
}
