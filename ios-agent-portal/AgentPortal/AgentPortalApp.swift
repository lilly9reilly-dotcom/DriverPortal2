import SwiftUI
import WebKit

@main
struct AgentPortalApp: App {
    var body: some Scene {
        WindowGroup {
            AgentWebView(url: AgentPortalConfig.startURL)
                .ignoresSafeArea()
        }
    }
}

enum AgentPortalConfig {
    /// غيّر إلى رابط Web App الحي عند الجاهزية.
    static let startURL: URL = {
        if let bundled = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "client-web") {
            return bundled
        }
        return URL(string: "https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec?page=admin")!
    }()
}

struct AgentWebView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.preferences.javaScriptEnabled = true
        config.websiteDataStore = .default()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.scrollView.keyboardDismissMode = .interactive
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, WKNavigationDelegate {
        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            decisionHandler(.allow)
        }
    }
}
