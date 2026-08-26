package io.github.norbertweb.bluebird.update

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mirrors the update.json hosted on GitHub.
 * All fields except versionCode / versionName are optional so older
 * manifests keep working.
 */
data class UpdateManifest(
    val versionCode: Int        = 0,
    val versionName: String     = "",
    val apkUrl: String          = "",
    val changelog: List<String> = emptyList(),
    val forceUpdate: Boolean    = false,
    val apkSize: String         = ""
)

/** What the update checker returns to callers. */
sealed class UpdateResult {
    object UpToDate                          : UpdateResult()
    data class UpdateAvailable(
        val manifest: UpdateManifest,
        val currentVersionCode: Int
    )                                        : UpdateResult()
    data class Error(val message: String)    : UpdateResult()
    object Loading                           : UpdateResult()
}

/**
 * How often the launcher should poll for updates automatically.
 * Stored as a string in SharedPreferences.
 */
enum class UpdateCheckFrequency(val label: String, val displayName: String) {
    EVERY_LAUNCH   ("every_launch",   "Every app launch"),
    DAILY          ("daily",          "Once a day"),
    WEEKLY         ("weekly",         "Once a week"),
    BIWEEKLY       ("biweekly",       "Every 2 weeks"),
    SEVEN_WEEKS    ("seven_weeks",    "Every 7 weeks"),
    MONTHLY        ("monthly",        "Monthly"),
    MANUAL         ("manual",         "Manual only");

    companion object {
        fun fromLabel(label: String) =
            entries.firstOrNull { it.label == label } ?: EVERY_LAUNCH
    }
}

/** Which update path the user prefers. */
enum class UpdateDelivery(val label: String, val displayName: String) {
    EXTERNAL ("external", "External — open GitHub release in browser"),
    INTERNAL ("internal", "In-app — download & install automatically");

    companion object {
        fun fromLabel(label: String) =
            entries.firstOrNull { it.label == label } ?: EXTERNAL
    }
}
