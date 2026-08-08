# The app is one Activity and a WebView. Nothing is reflected over, nothing is
# loaded by name, and there is no JavascriptInterface — the page and the host
# do not talk to each other at all, which is why there is no @JavascriptInterface
# keep rule here and why there must never need to be one.
#
# If you ever DO add a bridge, it needs:
#   -keepclassmembers class com.anticdrazelb.ringshift.* {
#       @android.webkit.JavascriptInterface <methods>;
#   }
# ...and it needs to be read very carefully first, because a bridge turns the
# page into an attack surface on the app.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
