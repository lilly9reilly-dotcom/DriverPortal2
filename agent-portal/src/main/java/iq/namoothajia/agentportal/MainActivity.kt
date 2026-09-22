package iq.namoothajia.agentportal

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * غلاف WebView لتطبيق المعتمدين.
 * المصدر: assets/client-web (نسخة من /client-web).
 */
class MainActivity : AppCompatActivity() {
  private lateinit var webView: WebView

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    webView = WebView(this)
    setContentView(webView)

    val settings: WebSettings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = true
    settings.allowContentAccess = true
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

    webView.webViewClient = WebViewClient()
    webView.webChromeClient = WebChromeClient()

    val portalUrl = intent?.getStringExtra(EXTRA_PORTAL_URL)
      ?: "file:///android_asset/client-web/index.html"
    webView.loadUrl(portalUrl)
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (this::webView.isInitialized && webView.canGoBack()) {
      webView.goBack()
    } else {
      @Suppress("DEPRECATION")
      super.onBackPressed()
    }
  }

  companion object {
    const val EXTRA_PORTAL_URL = "portal_url"
  }
}
