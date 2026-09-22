# تطبيق المعتمد — Android WebView

غلاف أندرويد لـ `client-web/`. رقم النسخة مستقل عن تطبيق السائق (لا يُرفع رقم متجر السائق).

## بناء APK
من جذر المشروع:
```bat
gradlew :agent-portal:assembleDebug
```
الملف: `agent-portal/build/outputs/apk/debug/agent-portal-debug.apk`

## المحتوى
- الواجهة من `src/main/assets/client-web/` (نسخة من `/client-web`)
- بعد ربط الحي: مرّر `portal_url` أو غيّر الافتراضي في `MainActivity`
