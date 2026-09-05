# Bluebird Store Submission & Repository Specification

## 1. Purpose

The Bluebird Store is the discovery/catalog layer for Bluebird applications. A `.bpk` package must remain independently installable.

The Store may be community-driven and hosted around an open-source Git repository.

---

# 2. Separation of concerns

```text
BPK package
   │
   ├── runtime identity
   ├── app files
   ├── icon
   └── optional installer

Store record
   │
   ├── discovery metadata
   ├── screenshots
   ├── category
   ├── release information
   ├── download URL
   └── integrity information
```

The Store should not need to modify the application payload.

---

# 3. Suggested repository layout

```text
bluebird-store/
├── apps/
│   ├── io.example.notes/
│   │   └── metadata.json
│   ├── io.example.tasks/
│   │   └── metadata.json
│   └── ...
├── index.json
├── categories.json
├── schemas/
│   └── metadata.schema.json
└── .github/
    ├── ISSUE_TEMPLATE/
    │   └── app-submission.yml
    └── workflows/
        └── validate-app.yml
```

The exact repository may differ, but the principle should remain: app catalog data is separate from the binary package itself.

---

# 4. Suggested Store metadata

Example:

```json
{
  "id": "io.example.notes",
  "name": "Example Notes",
  "description": "A lightweight notes app.",
  "developer": "Example Studio",
  "license": "MIT",
  "category": "Productivity",
  "homepage": "https://example.com",
  "source": "https://github.com/example/notes",
  "latestVersion": "1.0.0",
  "download": "https://github.com/example/notes/releases/download/v1.0.0/ExampleNotes.bpk",
  "sha256": "...",
  "screenshots": [
    "https://.../screenshot-1.png"
  ]
}
```

---

# 5. Submission workflow

Recommended community workflow:

```text
Developer
   ↓
Creates/test BPK
   ↓
Publishes source + release
   ↓
Opens App Submission issue or PR
   ↓
Automated validator
   ↓
Human/community review
   ↓
Merge catalog metadata
   ↓
Store index regenerated
```

Developers do not have to privately contact the project maintainer for every submission.

---

# 6. Automated validation

GitHub Actions can validate:

```text
ZIP/BPK integrity
Manifest syntax
Required fields
Required package paths
Safe relative paths
Required icon
Required entry point
Installer structure
File-count limits
Extracted size
Package hash
Version metadata
Store metadata schema
```

A validator can report:

```text
🐦 Bluebird Store Validator

✓ Package detected
✓ Manifest valid
✓ Icon valid
✓ Entry point found
✓ Installer valid
✓ No unsafe paths
✓ SHA-256 calculated

Status: READY FOR REVIEW
```

Or:

```text
✗ Missing icon/icon.png
✗ Manifest entry does not exist

Status: CHANGES REQUIRED
```

---

# 7. GitHub Releases

A developer can publish the package through a GitHub Release.

Recommended release assets:

```text
ExampleNotes.bpk
SHA256SUMS.txt
```

The Store metadata points at the release asset.

The package itself remains independently downloadable.

---

# 8. Integrity

The Store should record a SHA-256 hash for every published package.

Verification model:

```text
Store metadata hash
        ↓
Download BPK
        ↓
Calculate hash
        ↓
Compare
        ↓
Install only if expected
```

Publisher signatures can be added later.

---

# 9. Versions and updates

The Store should preserve version history.

Example:

```text
1.0.0
1.1.0
1.1.1
2.0.0
```

The application's stable ID remains unchanged across ordinary releases.

The Store should not silently replace an application's identity because the package filename changes.

---

# 10. Security review

Community review should pay special attention to:

- unexpected external network calls;
- suspicious installer scripts;
- requests for unavailable capabilities;
- path traversal attempts;
- obfuscated code;
- package tampering;
- misleading publisher identity;
- impersonation of existing applications.

Automated checks are not a replacement for community review.

---

# 11. Store categories

A catalog can define categories such as:

```text
Productivity
Education
Utilities
Development
Games
Graphics
Media
Internet
System
Other
```

Categories are Store metadata, not a requirement for BPK execution.

---

# 12. Screenshots and branding

Store listings can contain screenshots and marketing artwork without putting these files into the runtime BPK package.

The BPK should contain the resources required to install and run the application. The Store can contain additional discovery assets.

---

# 13. Community contribution model

A practical open workflow is:

```text
Issue → validator → PR → review → merge
```

Possible contribution rules:

- one app per submission;
- source repository required where appropriate;
- package release must be reproducible or explainable;
- version must match manifest;
- hash must match the release asset;
- metadata must follow schema;
- application must install successfully.

---

# 14. Index generation

The Store can generate a compact catalog for Bluebird clients:

```text
apps/
   ↓
metadata validation
   ↓
index generation
   ↓
index.json
```

Example:

```json
{
  "version": 1,
  "generatedAt": "2026-09-05T00:00:00Z",
  "apps": [
    {
      "id": "io.example.notes",
      "name": "Example Notes",
      "version": "1.0.0",
      "category": "Productivity"
    }
  ]
}
```

`generatedAt` and similar fields are catalog metadata and should not be required for local `.bpk` installation.

---

# 15. Local package installation

Bluebird must continue to support:

```text
User receives MyApp.bpk
       ↓
Opens it locally
       ↓
Bluebird installer
       ↓
Install
```

The Store should never become a mandatory gate for package installation.

---

# 16. Removal and moderation

A Store listing can be removed without making already-installed applications suddenly disappear.

A removal action should affect catalog availability and future downloads, subject to Bluebird's future security/update policy.

---

# 17. Future signed ecosystem

A mature Store can support:

```text
Source repository
      ↓
Build/release
      ↓
BPK hash
      ↓
Publisher signature
      ↓
Store review
      ↓
Store publication
      ↓
Bluebird verification
```

This should be added as a future security layer without breaking the basic local `.bpk` model.
