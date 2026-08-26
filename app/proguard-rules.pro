# Add project specific ProGuard rules here.
-keep class io.github.norbertweb.bluebird.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
