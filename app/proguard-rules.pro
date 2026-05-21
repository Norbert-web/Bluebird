# Add project specific ProGuard rules here.
-keep class com.bluebird.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
