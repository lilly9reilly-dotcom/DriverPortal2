/**
 * Android WebView shell for the client portal.
 * Browser-first portal remains the source of truth in /client-web.
 * This module is a packaging scaffold only — no store version bump.
 */
package iq.namoothajia.clientportal

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val webView = WebView(this)
    setContentView(webView)

    val settings: WebSettings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = true
    settings.builtInZoomControls = false
    settings.displayZoomControls = false

    webView.webViewClient = WebViewClient()
    webView.webChromeClient = WebChromeClient()

    // Demo/local asset path. Replace with https Apps Script / hosted URL after go-live.
    val portalUrl = intent?.getStringExtra(EXTRA_PORTAL_URL)
      ?: "file:///android_asset/client-web/index.html"
    webView.loadUrl(portalUrl)
  }

  companion object {
    const val EXTRA_PORTAL_URL = "portal_url"
  }
}
