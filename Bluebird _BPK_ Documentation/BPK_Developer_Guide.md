# Bluebird BPK Developer Guide

This guide explains how to build, package, test, distribute, and maintain a Bluebird `.bpk` application.

The BPK model turns a web application into a first-class Bluebird application rather than a browser bookmark or ordinary web tab.

---

# 1. What you are building

A BPK is a ZIP-based application package:

```text
MyApp.bpk
├── manifest.json
├── icon/
│   └── icon.png
└── app/
    └── index.html
```

The package is installed into Bluebird Storage, registered as an application, and represented by a generated Bluebird `.exe` launch descriptor.

The web app itself runs in Bluebird's web runtime inside a normal Bluebird application window.

There is no browser address bar, back button, or forward button in the application's Bluebird window.

---

# 2. Recommended project layout

Keep your source project separate from the final package:

```text
my-app/
├── src/
│   ├── index.html
│   ├── css/
│   ├── js/
│   └── assets/
├── package/
│   ├── manifest.json
│   ├── icon/
│   │   └── icon.png
│   ├── app/
│   │   └── index.html
│   └── installer/
│       ├── index.html
│       ├── style.css
│       └── installer.js
└── README.md
```

The final `.bpk` should contain the **contents of `package/`**, not the `package` directory itself.

---

# 3. Create the manifest

Start with:

```json
{
  "id": "io.example.myapp",
  "name": "My App",
  "version": "1.0.0",
  "publisher": "Example",
  "runtime": "web",
  "entry": "app/index.html",
  "icon": "icon/icon.png",
  "description": "A Bluebird application."
}
```

Required fields:

```text
id
name
version
publisher
runtime
entry
icon
```

Use a stable application ID. Do not change it for every release.

---

# 4. Build the web application

Use ordinary HTML/CSS/JavaScript.

Example `app/index.html`:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <link rel="stylesheet" href="css/app.css">
  <title>My App</title>
</head>
<body>
  <main>
    <h1>My App</h1>
    <button id="hello">Hello</button>
  </main>
  <script src="js/app.js"></script>
</body>
</html>
```

Use relative asset paths:

```html
<link rel="stylesheet" href="css/app.css">
<script src="js/app.js"></script>
```

Avoid hard-coding an absolute Bluebird Storage path.

---

# 5. Add the icon

Place your application icon at:

```text
icon/icon.png
```

The manifest points to it:

```json
"icon": "icon/icon.png"
```

Bluebird can read this icon from a BPK so File Explorer/Desktop can display the package's actual icon before installation.

The installed application uses the same application identity/icon throughout Bluebird's shell.

---

# 6. Add a custom installer

A custom installer is optional.

Use:

```text
installer/
├── index.html
├── style.css
└── installer.js
```

All three are required if the folder exists.

The installer is rendered inside Bluebird's installation window.

You design:

- welcome screen;
- artwork;
- information layout;
- installation-location presentation;
- options;
- progress presentation;
- completion screen.

Bluebird performs the actual installation operations.

---

# 7. Installer bridge

The simplest current calls are:

```javascript
const manifest = JSON.parse(
  BluebirdInstaller.getManifest()
);

BluebirdInstaller.install();
BluebirdInstaller.cancel();
```

The target richer API is:

```javascript
BluebirdInstaller.getManifest();
BluebirdInstaller.getPackageInfo();
BluebirdInstaller.getInstallPath();
BluebirdInstaller.setInstallPath(path);
BluebirdInstaller.getInstallOptions();
BluebirdInstaller.setInstallOptions(options);
BluebirdInstaller.getProgress();
BluebirdInstaller.install();
BluebirdInstaller.cancel();
```

Treat richer APIs as version-dependent until your minimum supported Bluebird version provides them.

---

# 8. Installation options

The Bluebird installation experience separates shell destinations.

A package can request, through its installer UI or Bluebird's standard controls:

```javascript
{
  addToStart: false,
  pinToTaskbar: false,
  createDesktopShortcut: false
}
```

These are independent choices.

A user who selects **Pin to Start** must not automatically pin the application to Taskbar.

A user who selects **Create desktop shortcut** does not uninstall or move the application when the shortcut is later deleted.

---

# 9. Installation path

The default location is conceptually:

```text
Bluebird Storage/
└── Program Files/
    └── My App/
```

The user may select a different valid Bluebird installation directory during installation.

Your application must therefore work regardless of the selected path.

Never assume:

```text
/storage/emulated/0/.../Program Files/My App
```

or any other device-specific absolute path.

---

# 10. Installed application

After installation:

```text
Program Files/
└── My App/
    ├── My App.exe
    ├── manifest.json
    ├── icon/
    ├── app/
    └── installer/
```

`My App.exe` is the Bluebird launch descriptor.

It is not native Windows code.

---

# 11. How launching works

Every launch surface resolves the same installed application identity.

```text
Start ───────────┐
Taskbar ─────────┤
Desktop ─────────┤
File Explorer ───┤
Program Manager ┤
Store ───────────┤
                ▼
        Bluebird application
                │
                ▼
        Bluebird Launcher
                │
                ▼
          Window Manager
                │
                ▼
           Web runtime
                │
                ▼
       app/index.html
```

Opening the `.exe` from its own application folder is also a supported launch model.

---

# 12. Do not treat the application as a browser

Do not design the application around a visible URL field.

The Bluebird window already provides application identity through its title bar and shell integration.

Internal links work normally inside your application.

For an app that needs navigation, implement navigation controls inside your own UI instead of expecting Bluebird to provide browser controls.

---

# 13. User data

Do not write user-generated settings, documents, caches, or databases into your installed `Program Files` payload.

Prefer a user-data location conceptually like:

```text
Bluebird Storage/
└── Users/
    └── <user>/
        └── AppData/
            └── io.example.myapp/
```

For purely browser-style applications, runtime-supported `localStorage`/IndexedDB-style storage may be appropriate where available.

The important rule is to distinguish:

```text
Application payload ≠ User data
```

This makes updates, repair and reinstall safer.

---

# 14. Designing for updates

Do not embed the version into the application ID.

Good:

```text
id = io.example.notes
version = 1.0.0
```

then:

```text
id = io.example.notes
version = 1.1.0
```

Bad:

```text
io.example.notes.v1
io.example.notes.v2
```

unless you are intentionally publishing a completely different application.

---

# 15. Uninstall/reinstall/repair expectations

An application should remain self-contained and replaceable.

### Uninstall

Expect Bluebird to remove its installed application files and shell references.

### Reinstall

Expect the same application identity to be retained.

### Repair

Expect the application payload to be restored without intentionally destroying user data.

Do not make your app's user data depend on files inside `Program Files`.

---

# 16. Security rules

Treat all package input as untrusted.

Do not attempt to:

- access Bluebird's internal registry directly;
- access arbitrary Android filesystem paths from installer JavaScript;
- escape the package root with `../`;
- assume a package can execute native Android code;
- assume a BPK `.exe` is a real Windows executable.

Only use explicitly documented Bluebird APIs.

---

# 17. File paths that break packages

Avoid:

```text
../config.json
../../secret.txt
/storage/emulated/0/MyApp/data.db
C:\something\file.txt
```

Prefer:

```text
assets/data.json
css/app.css
js/app.js
```

---

# 18. Build the BPK

The final ZIP structure must be:

```text
manifest.json
icon/icon.png
app/index.html
...
```

Do **not** accidentally produce:

```text
MyApp/
  manifest.json
  icon/
  app/
```

inside the ZIP unless Bluebird's package tool explicitly strips that outer directory.

Then rename:

```text
MyApp.zip
```

to:

```text
MyApp.bpk
```

---

# 19. Test checklist

Before distributing:

```text
[ ] Opens as .bpk in Bluebird
[ ] Manifest validates
[ ] Icon displays correctly
[ ] Installer opens
[ ] Installer uses one Bluebird window
[ ] Install path is displayed correctly
[ ] User can change installation location where supported
[ ] Install completes
[ ] Generated .exe exists
[ ] Start shortcut works when selected
[ ] Taskbar pin works when selected
[ ] Desktop shortcut works when selected
[ ] Selecting Start does not silently select Taskbar
[ ] App opens from File Explorer .exe
[ ] App opens from its installed folder
[ ] App window has no browser URL bar
[ ] Application works after Bluebird restart
[ ] User data survives application update/reinstall where applicable
[ ] Uninstall removes the application correctly
```

---

# 20. Recommended development workflow

```text
1. Build web app
2. Test it as a normal web application
3. Add manifest
4. Add icon
5. Put entry point under app/
6. Optionally build installer/
7. Package as ZIP
8. Rename to .bpk
9. Install into a clean Bluebird environment
10. Test Start/Taskbar/Desktop/File Explorer paths
11. Test reinstall/uninstall behavior
12. Publish
```

---

# 21. Store-ready release

The same `.bpk` can be hosted outside Bluebird Store.

Possible channels:

```text
Bluebird Store
GitHub Releases
Developer website
Direct download
```

Store metadata can add:

- screenshots;
- category;
- release notes;
- license;
- source URL;
- package hash;
- minimum Bluebird version.

Keep the runtime manifest usable even when the package is installed directly.

---

# 22. Example complete package

```text
BluebirdNotes.bpk
├── manifest.json
├── icon/
│   └── icon.png
├── app/
│   ├── index.html
│   ├── css/
│   │   └── notes.css
│   ├── js/
│   │   └── notes.js
│   └── assets/
│       └── empty.png
└── installer/
    ├── index.html
    ├── style.css
    └── installer.js
```

Manifest:

```json
{
  "id": "io.example.bluebirdnotes",
  "name": "Bluebird Notes",
  "version": "1.0.0",
  "publisher": "Example Studio",
  "runtime": "web",
  "entry": "app/index.html",
  "icon": "icon/icon.png",
  "description": "A simple offline notes application."
}
```

---

# 23. Store submission preparation

Before submitting a BPK to a community catalog, prepare:

```text
Application name
Application ID
Version
Publisher
Description
License
Source repository
Homepage
Screenshots
BPK download/release
SHA-256 hash
Minimum Bluebird version (when supported)
```

The Store should be able to validate the package independently of its listing metadata.
