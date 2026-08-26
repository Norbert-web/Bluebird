package io.github.norbertweb.bluebird.core.filesystem


import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * BluebirdFileSystem
 * ───────────────────
 * Single source of truth for Bluebird's virtual, Windows-like filesystem.
 *
 * Bluebird Terminal, Bluebird Files, and any other Bluebird app should
 * go through this object rather than building java.io.File paths by
 * hand — that's what keeps the "C:\" view terminal shows the user
 * consistent with what Files (and everything else) sees, and it's
 * what enforces the security boundary from the design doc: Bluebird
 * apps can only ever see inside Bluebird/, never the rest of the
 * device.
 *
 * Virtual root "C:\" maps to <ExternalStorage>/Bluebird/
 *
 * SCOPED STORAGE NOTE: this mirrors the original TerminalScreen.kt's
 * use of Environment.getExternalStorageDirectory(), which needs
 * legacy external storage / MANAGE_EXTERNAL_STORAGE on modern Android.
 * If Bluebird targets API 30+, the only thing that should need to
 * change is the `root` property below (e.g. swap in
 * context.getExternalFilesDir(null), which needs no special
 * permission) — every command and every other file in the app keeps
 * working unchanged because they only ever talk to this object.
 */
object BluebirdFileSystem {

    /** Currently logged-in Bluebird user. Hardcoded for now; every
     *  command reads this instead of the literal string "Bluebird" so
     *  multi-user support (C:\Users\Student> etc., per the design doc)
     *  is just swapping this value later — nothing else changes. */
    var currentUser: String = "Bluebird"
        private set

    fun setCurrentUser(name: String) {
        currentUser = name
        ensureStructure(name)
    }

    /** Real on-device root: <ExternalStorage>/Bluebird */
    val root: File
        get() = File(Environment.getExternalStorageDirectory(), "Bluebird")

    private fun usersRoot() = File(root, "Users")
    fun homeDir(user: String = currentUser): File = File(usersRoot(), user)

    private val standardUserDirs = listOf(
        "Desktop", "Documents", "Downloads", "Music", "Pictures", "Videos", "AppData"
    )

    /** The planned Bluebird app suite. Used both to seed Program Files/
     *  and as the fallback list for the Terminal's `apps` command until
     *  a real App Registry (Phase 3) exists. */
    val bluebirdApps = listOf(
        "Bluebird Writer", "Bluebird Files", "Bluebird Music",
        "Bluebird Calculator", "Bluebird Settings", "Bluebird Terminal"
    )

    private val systemDirs = listOf("Config", "Themes", "Fonts", "Services", "Logs")
    private val sharedDirs = listOf("Templates", "Fonts", "Documents", "Resources")

    /**
     * Creates the full Bluebird/ tree on device storage if any part of
     * it is missing. Cheap and idempotent — safe to call on every
     * Terminal/Files launch, it only creates what doesn't already
     * exist. This is what makes "the terminal creates it all if it's
     * not there" true without needing a separate first-run installer.
     */
    fun ensureStructure(user: String = currentUser) {
        mkdirs(root)
        mkdirs(File(root, "Program Files"))
        mkdirs(File(root, "System"))
        mkdirs(File(root, "Shared"))
        mkdirs(File(root, "Temp"))

        mkdirs(usersRoot())
        val home = homeDir(user)
        mkdirs(home)
        standardUserDirs.forEach { mkdirs(File(home, it)) }

        bluebirdApps.forEach { mkdirs(File(root, "Program Files/$it")) }
        systemDirs.forEach { mkdirs(File(root, "System/$it")) }
        sharedDirs.forEach { mkdirs(File(root, "Shared/$it")) }
    }

    private fun mkdirs(f: File) {
        if (!f.exists()) f.mkdirs()
    }

    // ── Virtual <-> real path translation ───────────────────────────

    /** Collapses ../ segments etc. via the JVM's own canonicalization,
     *  falling back to the un-normalized file if that fails. */
    private fun normalize(f: File): File =
        try { File(f.canonicalPath) } catch (_: Exception) { f }

    /** The security boundary: every mutating command must check this
     *  before touching a resolved path. A path outside Bluebird/ is
     *  never valid input, regardless of what the user typed. */
    fun isInsideBluebird(real: File): Boolean =
        try {
            real.canonicalPath == root.canonicalPath ||
                real.canonicalPath.startsWith(root.canonicalPath + File.separator)
        } catch (_: Exception) {
            false
        }

    /** Free space on the volume backing the Bluebird root, for `dir`
     *  and `vol` output. */
    fun freeBytes(): Long =
        try { StatFs(root.absolutePath).freeBytes } catch (_: Exception) { 0L }

    // ── Explorer / "admin" mode ─────────────────────────────────────
    //
    // C:\ stays a hard sandbox boundary always — that's the whole
    // point of this class. D:\ is a second, EXPLICIT drive that only
    // resolves when the caller passes allowDeviceDrive = true (i.e.
    // the terminal's `admin` toggle is on). It maps to the same real
    // external storage root the app already needs storage permission
    // for — this is not, and can't be, true root/system filesystem
    // access; Android doesn't allow that without a rooted device, and
    // nothing here tries to get around that.

    /** Real device storage root — what D:\ maps to when explorer mode is on. */
    val deviceRoot: File
        get() = Environment.getExternalStorageDirectory()

    fun toReal(path: String, currentVirtualDir: File = homeDir(), allowDeviceDrive: Boolean = false): File {
        val cleaned = path.trim()
        return when {
            cleaned.isEmpty() -> currentVirtualDir
            cleaned.equals("C:\\", ignoreCase = true) ||
                cleaned.equals("C:", ignoreCase = true) ||
                cleaned == "\\" -> root
            cleaned.startsWith("C:\\", ignoreCase = true) ->
                File(root, cleaned.substring(3).replace('\\', '/'))
            allowDeviceDrive && (cleaned.equals("D:\\", ignoreCase = true) || cleaned.equals("D:", ignoreCase = true)) ->
                deviceRoot
            allowDeviceDrive && cleaned.startsWith("D:\\", ignoreCase = true) ->
                File(deviceRoot, cleaned.substring(3).replace('\\', '/'))
            cleaned == ".." -> currentVirtualDir.parentFile ?: currentVirtualDir
            cleaned == "~" -> homeDir()
            else -> File(currentVirtualDir, cleaned.replace('\\', '/'))
        }.let { normalize(it) }
    }

    /** Access check that accounts for D:\ when explorer mode is on.
     *  Every command must go through this (or the C:\-only overload
     *  above) — never through a raw File check. */
    fun isAccessAllowed(real: File, allowDeviceDrive: Boolean): Boolean =
        isInsideBluebird(real) || (allowDeviceDrive && try {
            real.canonicalPath == deviceRoot.canonicalPath ||
                real.canonicalPath.startsWith(deviceRoot.canonicalPath + File.separator)
        } catch (_: Exception) { false })

    fun toVirtual(real: File, allowDeviceDrive: Boolean = false): String {
        val rootPath = root.absolutePath
        val devicePath = deviceRoot.absolutePath
        val realPath = real.absolutePath
        return when {
            realPath == rootPath -> "C:\\"
            realPath.startsWith("$rootPath/") -> "C:" + realPath.removePrefix(rootPath).replace('/', '\\')
            allowDeviceDrive && realPath == devicePath -> "D:\\"
            allowDeviceDrive && realPath.startsWith("$devicePath/") ->
                "D:" + realPath.removePrefix(devicePath).replace('/', '\\')
            else -> realPath
        }
    }
}
