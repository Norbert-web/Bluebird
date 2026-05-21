# Add project specific ProGuard rules here.
-keep class com.win11launcher.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
