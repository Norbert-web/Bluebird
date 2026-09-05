# Add project specific ProGuard rules here.

# --- Phase 1: safe first pass ---
# Keeps your own app code untouched for now (own-code shrinking is a
# separate follow-up step once this build is verified stable).
-keep class io.github.norbertweb.bluebird.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- Gson ---
# Gson deserializes by reflecting into field names — without these rules,
# R8 can rename/strip fields and break JSON parsing silently at runtime
# (not a compile error). Keep this broad until you confirm exact model
# classes; narrow it later to just your @SerializedName-annotated classes.
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# --- WebView JS interface ---
# Only needed if any addJavascriptInterface() call exists (browser app).
# Safe to leave in even if unused.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# --- Room ---
# Room bundles its own consumer-rules.pro, but keeping entities explicitly
# avoids edge cases with reflection-based migrations.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Media3 ---
# media3-session in particular is sensitive to R8 aggressively stripping
# MediaSessionService callbacks; media3 ships consumer rules but this is
# a known trouble spot worth a safety net.
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.session.MediaSession$Callback { *; }
