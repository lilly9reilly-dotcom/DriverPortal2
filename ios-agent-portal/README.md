# تطبيق المعتمد — iOS (WebView)

غلاف SwiftUI/WKWebView منفصل عن تطبيق السائق (`ios/DriverPortalIOS`).

## على جهاز Mac
```bash
cd ios-agent-portal
# اختياري: انسخ client-web إلى AgentPortal/client-web
xcodegen generate
xcodebuild -scheme AgentPortal -configuration Release -sdk iphoneos -derivedDataPath build
```

بناء IPA يحتاج حساب توقيع Apple على macOS. على Windows نُجهّز المصدر فقط.
