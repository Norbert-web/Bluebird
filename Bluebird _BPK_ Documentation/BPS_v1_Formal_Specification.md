# Bluebird Package Specification (BPS) v1

## Status

This document is the formal developer specification for the Bluebird Package (`.bpk`) format.

**Current runtime:** `web`  
**Container:** ZIP  
**Package extension:** `.bpk`

This specification distinguishes between **Implemented in BPS v1** and **Reserved/Planned** behavior. The latter is documented so developers can design packages without relying on undocumented conventions.

---

## 1. Package identity

A BPK is a ZIP container renamed with the `.bpk` extension. The package contains a self-contained Bluebird application.

A BPK application is installed as a first-class Bluebird application. It may be launched from Start, Taskbar, Desktop, File Explorer, Program Manager, the Bluebird Store, or directly from its installed `.exe` launcher descriptor.

The `.exe` file is a Bluebird launch descriptor. It is **not** a Windows PE executable.

---

## 2. Required package structure

```text
MyApp.bpk
├── manifest.json
├── icon/
│   └── icon.png
└── app/
    └── index.html
```

`app/` may contain additional application files:

```text
app/
├── index.html
├── css/
├── js/
├── assets/
├── fonts/
└── data/
```

### Optional installer

```text
installer/
├── index.html
├── style.css
└── installer.js
```

When `installer/` is present, all three files are required.

---

## 3. Package paths

All package paths are relative POSIX-style paths using `/` as the separator.

Forbidden path forms include:

- absolute paths
- drive-letter paths
- paths containing `..` traversal
- paths that resolve outside the package root
- duplicate/conflicting paths after normalization

Examples:

```text
app/index.html        valid
app/pages/home.html   valid
../outside.html       invalid
../../outside.html    invalid
/absolute/file        invalid
C:/outside/file       invalid
```

---

## 4. `manifest.json`

### Required fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `id` | string | Yes | Stable unique application identifier |
| `name` | string | Yes | Human-readable application name |
| `version` | string | Yes | Application/package version |
| `publisher` | string | Yes | Publisher/developer name |
| `runtime` | string | Yes | Current value: `web` |
| `entry` | string | Yes | Relative application entry point |
| `icon` | string | Yes | Relative package icon path |
| `description` | string | No | Application description |
| `homepage` | string | No | Developer/application homepage |

### Canonical example

```json
{
  "id": "io.example.myapp",
  "name": "My App",
  "version": "1.0.0",
  "publisher": "Example",
  "runtime": "web",
  "entry": "app/index.html",
  "icon": "icon/icon.png",
  "description": "Example Bluebird application.",
  "homepage": "https://example.com"
}
```

### Application ID

IDs should use a stable reverse-domain-style namespace:

```text
io.example.myapp
com.example.notes
org.example.pixelstudio
```

The ID identifies the application across releases. Do not change it merely because the version changes.

### Version

Use a predictable release version such as:

```text
1.0.0
1.1.0
2.0.0
```

BPS v1 does not require a specific semantic-versioning parser, but developers should avoid ambiguous version strings.

---

## 5. Runtime

The current supported runtime is:

```json
"runtime": "web"
```

Bluebird opens a BPK web application through its application runtime and Window Manager.

A BPK application is not presented as a normal browser tab. Its window does not expose a browser URL/address bar, back button, or forward button as part of the Bluebird application chrome.

Future runtimes may be defined in later BPS revisions. Unknown runtime values must not be assumed to be executable.

---

## 6. Application entry point

The `entry` field points to the initial HTML document.

```json
"entry": "app/index.html"
```

The entry point must:

1. exist in the package;
2. remain inside the package root after path normalization;
3. be a file;
4. be loadable by the `web` runtime.

Relative references inside the application should preferably remain package-relative.

---

## 7. Application icon

The mandatory icon is:

```text
icon/icon.png
```

The package icon is the canonical visual identity used by Bluebird wherever the installed application is represented, including the application installer, Desktop, File Explorer, Start, Taskbar, Program Manager, and application window identity.

Developers should use a square PNG with transparency where appropriate and provide sufficient resolution for both small shell icons and larger application surfaces.

The precise dimension/size limits are implementation-defined in BPS v1 and may be tightened by a future validator version.

---

## 8. Application files

BPK web applications may use ordinary web technologies:

- HTML
- CSS
- JavaScript
- JSON
- images
- SVG
- fonts
- audio/video assets supported by the runtime
- other static resources supported by the runtime

Keep resource references relative to the application whenever possible.

Example:

```html
<link rel="stylesheet" href="css/app.css">
<script src="js/app.js"></script>
<img src="assets/logo.png" alt="Logo">
```

---

## 9. Developer-designed installer

A package can provide a custom installation UI:

```text
installer/
├── index.html
├── style.css
└── installer.js
```

The installer is displayed inside Bluebird's installation window and executes in a sandboxed web context. It does not receive unrestricted Android or filesystem access.

### Current bridge

```javascript
BluebirdInstaller.getManifest()
BluebirdInstaller.install()
BluebirdInstaller.cancel()
```

`getManifest()` returns the manifest as JSON text.

### Extended installer API

The following API is the target interface for the richer installer experience and may be implemented incrementally by Bluebird versions:

```javascript
BluebirdInstaller.getManifest()
BluebirdInstaller.getPackageInfo()
BluebirdInstaller.getInstallPath()
BluebirdInstaller.setInstallPath(path)
BluebirdInstaller.getInstallOptions()
BluebirdInstaller.setInstallOptions(options)
BluebirdInstaller.getProgress()
BluebirdInstaller.install()
BluebirdInstaller.cancel()
```

Suggested option object:

```javascript
{
  "addToStart": false,
  "pinToTaskbar": false,
  "createDesktopShortcut": false
}
```

Bluebird remains authoritative over whether an operation is allowed.

---

## 10. Installer responsibilities

The custom HTML installer is the **presentation layer**.

The Bluebird installation engine remains responsible for:

- package validation;
- safe extraction;
- installation path validation;
- copying/extraction;
- optimization;
- executable descriptor creation;
- icon handling;
- application registration;
- shell integration;
- cleanup on failure;
- uninstall/update/reinstall operations.

JavaScript must not be trusted merely because it was shipped in a BPK.

---

## 11. Default installation location

The canonical default layout is:

```text
Bluebird Storage/
└── Program Files/
    └── My App/
```

The final directory name is derived from the installed application name according to Bluebird's filesystem-safe naming rules.

The user may change the installation directory during installation when the installer exposes path selection.

The chosen directory becomes the installed application's registered `installPath`.

A package must not assume every installation is located at the default path.

---

## 12. Installed application layout

A normal installation looks like:

```text
Bluebird Storage/
└── Program Files/
    └── My App/
        ├── My App.exe
        ├── manifest.json
        ├── icon/
        │   └── icon.png
        ├── app/
        │   ├── index.html
        │   └── ...
        └── installer/
            └── ...
```

The package remains self-contained after installation.

---

## 13. Application data

Developers should **not** store user-generated data inside the installed `Program Files` application directory.

Reserved architecture for application data:

```text
Bluebird Storage/
└── Users/
    └── <user>/
        └── AppData/
            └── <application-id>/
```

This user-data layout is reserved for application-state isolation and should be preferred by future BPK APIs over modifying packaged application files.

Web applications that need only browser-style storage may use the runtime's supported storage mechanisms.

---

## 14. Bluebird executable descriptor

During installation Bluebird creates:

```text
My App.exe
```

This is a Bluebird launch descriptor and should not be confused with a native Windows executable.

It records enough information for Bluebird to resolve the application, including:

- Bluebird executable magic/version;
- application ID;
- application name;
- relative source-root reference;
- entry-point reference;
- icon path;
- embedded Base64 PNG icon.

The source root is relative to the `.exe` so the installed directory can be moved as an intact application installation when supported by the shell/registry layer.

Opening the `.exe` from Desktop, File Explorer, or another Bluebird surface resolves the same installed application and launches it through the Bluebird Window Manager.

---

## 15. Application registry

Bluebird maintains an installed-application registry, currently represented by:

```text
Bluebird Storage/
└── ProgramData/
    └── installed-apps.json
```

The registry is the shared identity layer for installed applications.

A record should conceptually contain:

```text
id
name
version
publisher
runtime
iconPath
installPath
exePath
entryPoint
packagePath
installationState
capabilities
fileAssociations
```

Not every field is required by BPS v1 implementation.

The registry is intended to be the source of truth for Start, Search, Taskbar, Desktop shortcuts, File Explorer, Program Manager, and future update mechanisms.

---

## 16. Shell integration

### Start

A package may be added to Start by the user's installation choice or later through Bluebird shell controls.

### Taskbar

Taskbar pinning is independent from Start pinning. A package must never be added to Taskbar merely because it was added to Start.

### Desktop

A desktop shortcut may reference the installed `.exe`.

### File Explorer

The `.bpk` package can display its package icon before installation. The installed `.exe` displays the application identity/icon associated with its BPK.

### Program Manager

Installed BPK applications appear as applications that can be launched and, where supported, reinstalled, repaired, updated, or uninstalled.

---

## 17. Installation lifecycle

```text
BPK file
  ↓
Package validation
  ↓
Manifest/icon/entry validation
  ↓
Installer UI
  ↓
Installation path selection
  ↓
User options
  ↓
Safe extraction
  ↓
Web asset optimization
  ↓
Executable descriptor creation
  ↓
Application registration
  ↓
Start/Taskbar/Desktop actions
  ↓
Installation complete
```

A failed installation should not leave an apparently-installed registry entry without its required files.

---

## 18. Uninstall lifecycle

A complete uninstall should conceptually:

1. stop/close running application instances;
2. remove or update shell shortcuts/pins;
3. remove the registry entry;
4. remove installed application files;
5. preserve application user data when the user chooses to retain it and the application supports that model.

The exact UI and data-retention policy may evolve with Program Manager.

---

## 19. Reinstall and repair

### Reinstall

Reinstall replaces the application payload while preserving the application identity and, where supported, user data.

### Repair

Repair restores missing/corrupt installed application files from a trusted package without intentionally deleting user data.

A future package revision may declare whether repair/reinstall is supported.

---

## 20. Validation and security

Bluebird validates:

- ZIP/container integrity;
- required directories/files;
- manifest fields;
- application entry point;
- icon presence;
- path safety;
- ZIP path traversal;
- file-count limits;
- extracted-size limits;
- custom installer structure;
- safe installation destination.

### Untrusted web content

BPK HTML/JavaScript must be treated as untrusted content. The installer/runtime bridge should expose only explicitly approved operations.

The package must not rely on direct access to Bluebird's internal files.

---

## 21. Optimization

Bluebird may perform conservative optimization when installing a BPK.

Current policy:

- HTML: conservative minification may be performed;
- CSS: conservative minification may be performed;
- JavaScript: arbitrary naive minification is avoided because it can break applications;
- binary assets: may be copied without modification unless a future optimizer explicitly supports them.

Developers should not depend on whitespace remaining byte-for-byte identical after installation.

---

## 22. File associations — reserved

Future BPS versions may allow applications to declare supported file extensions, for example:

```text
.txt
.myformat
```

Possible future manifest shape:

```json
"fileAssociations": [
  {
    "extension": ".myformat",
    "description": "My Format"
  }
]
```

This is reserved and should not be treated as a guaranteed BPS v1 field unless implemented by the target Bluebird release.

---

## 23. Capabilities — reserved

A future BPS revision may allow applications to declare requested capabilities, for example:

```text
network
notifications
clipboard
camera
microphone
location
storage
```

Possible future shape:

```json
"capabilities": [
  "network",
  "notifications"
]
```

Capabilities should be declarative and permission-controlled. Declaring a capability must not imply automatic access.

---

## 24. Package integrity and signing — reserved

For future Store distribution, BPK packages should support integrity metadata such as:

```text
SHA-256(package)
```

and eventually publisher signatures.

The intended verification flow is:

```text
Download
  ↓
Hash verification
  ↓
Optional publisher signature verification
  ↓
Package validation
  ↓
Install
```

These mechanisms are not a requirement for today's minimal BPS v1 package structure.

---

## 25. Store metadata — separate from runtime manifest

The BPK manifest describes the installed application.

The Bluebird Store catalog may additionally contain:

- screenshots;
- category;
- store description;
- license;
- supported Bluebird version;
- package download URL;
- package hash;
- release notes;
- source repository.

Store metadata should not be required merely to run a local `.bpk`.

---

## 26. Compatibility

Future versions may add:

```json
"minimumBluebirdVersion": "3.0.0"
```

and runtime/API compatibility declarations.

Unknown manifest fields should be ignored by tolerant readers unless they affect safety. Unknown runtime values must not be executed.

---

## 27. Package distribution

A `.bpk` may be distributed through:

- Bluebird Store;
- GitHub Releases;
- a developer website;
- direct file transfer;
- other trusted distribution channels.

The Bluebird Store is a discovery/catalog layer, not the only installation source.

---

## 28. Authoring and packaging

A developer can create a BPK with any web-development stack as long as the final package satisfies this specification.

Minimum workflow:

```text
Create web app
  ↓
Create manifest.json
  ↓
Create icon/icon.png
  ↓
Create app/index.html
  ↓
Add remaining app assets
  ↓
Optionally add installer/
  ↓
Validate structure
  ↓
ZIP package contents
  ↓
Rename .zip → .bpk
  ↓
Test in Bluebird
```

The ZIP must contain `manifest.json`, `icon/`, and `app/` at its root. Do not wrap the entire tree inside an additional outer directory.

---

## 29. Reference package

```text
MyApp.bpk
├── manifest.json
├── icon/
│   └── icon.png
├── app/
│   ├── index.html
│   ├── css/
│   │   └── app.css
│   ├── js/
│   │   └── app.js
│   └── assets/
│       └── logo.png
└── installer/
    ├── index.html
    ├── style.css
    └── installer.js
```

---

## 30. Conformance checklist

A package is structurally ready for BPS v1 when:

```text
[ ] File extension is .bpk
[ ] Package is a ZIP container
[ ] manifest.json exists at package root
[ ] icon/icon.png exists
[ ] app/index.html exists
[ ] manifest contains all required fields
[ ] manifest entry path exists
[ ] manifest icon path exists
[ ] all paths are safe and relative
[ ] no .. traversal exists
[ ] optional installer contains all three required files
[ ] application works from a clean installation
[ ] application does not depend on the default installation directory
```

---

## 31. Versioning of this specification

This document is **BPS v1**. Future incompatible changes should increment the specification major version.

Bluebird implementations may add stricter validation, additional manifest fields, capabilities, file associations, signatures, and richer installer APIs in future releases.
