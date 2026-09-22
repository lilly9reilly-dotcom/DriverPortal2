# Add keep rules only if minify is enabled later.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
