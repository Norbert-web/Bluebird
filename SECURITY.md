# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅ Active support |
| < 1.0   | ❌ No longer supported |

## Reporting a Vulnerability

If you discover a security vulnerability in Bluebird, **please do not open a public GitHub issue**. Instead:

1. **Email**: trebronwayne@gmail.com
2. **Subject**: `[SECURITY] Bluebird - Brief description`
3. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes (optional)

You will receive a response within **48 hours** acknowledging your report. We aim to release a patch within **14 days** for critical vulnerabilities.

## Scope

The following are **in scope** for security reports:

- Data leakage (e.g. SMS content, contacts exposed to other apps)
- Permission escalation
- Insecure storage of user data (username, profile picture, wallpaper)
- Intent vulnerabilities (unvalidated implicit intents)
- FileProvider path traversal

The following are **out of scope**:

- Issues requiring physical access to an unlocked device
- Issues in third-party libraries (report those upstream)
- Android OS-level vulnerabilities

## Data Bluebird Stores

Bluebird stores the following data locally on-device in `SharedPreferences` (not encrypted):

- Username (text string)
- Profile picture path (file path to internal storage copy)
- Wallpaper file paths (internal storage)
- Pinned app package names
- Desktop shortcut definitions (file paths and package names)
- Recycle Bin item metadata (file paths, deletion timestamps)
- Theme preferences (dark/light, accent color)

**Bluebird does not currently**:
- Transmit any user data to external servers
- Store SMS message content
- Store contact information
- Access the internet (except the built-in WebView browser, which uses the system's network stack)

## Permissions and Privacy

Bluebird requests sensitive permissions (SMS, Contacts, Call Log, Camera). These are used exclusively for the built-in Phone, Messages, and profile picture features. No data is shared with any third party.
