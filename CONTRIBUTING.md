Security Policy

📦 Supported Versions

Version| Supported
1.0.x| ✅ Active support
< 1.0| ❌ No longer supported

---

🔐 Reporting a Vulnerability

If you discover a security vulnerability in Bluebird, please do not open a public GitHub issue.

Instead, report it privately using the contact information below.

Contact

- Email: trebronwayne@gmail.com
- Subject: "[SECURITY] Bluebird - Short description"

Please Include

- A clear description of the vulnerability
- Steps to reproduce the issue
- Expected impact
- Screenshots, logs, or proof-of-concept code (if available)
- Suggested mitigation or fix (optional)

---

⏱️ Response Timeline

Bluebird aims to:

- Acknowledge vulnerability reports within 48 hours
- Provide status updates during investigation
- Release fixes for critical vulnerabilities as quickly as reasonably possible

Response times may vary depending on issue complexity and maintainer availability.

---

🎯 Security Scope

The following are considered in scope for responsible disclosure:

- Exposure of SMS or contact data
- Permission escalation vulnerabilities
- Unsafe file access or path traversal
- Exported activity/service vulnerabilities
- Intent spoofing or unvalidated implicit intents
- Insecure handling of wallpapers, profile images, or local user data
- FileProvider misconfiguration
- Privilege misuse involving launcher functionality
- Overlay or notification security bypasses

The following are generally out of scope:

- Vulnerabilities requiring physical access to an unlocked device
- Android OS or OEM firmware vulnerabilities
- Issues caused exclusively by rooted devices
- Vulnerabilities in third-party libraries or upstream Android components
- Social engineering attacks
- Denial-of-service caused by unsupported device modifications

---

🗄️ Data Storage & Privacy

Bluebird stores limited application data locally on-device using Android app-private storage.

This may include:

- Username
- Profile image path
- Wallpaper file paths
- Pinned apps
- Desktop shortcuts
- Recycle Bin metadata
- Theme and personalization preferences

At this time, Bluebird does not collect or transmit user data to external backend services.

Bluebird does not:

- Upload contacts or SMS data
- Store SMS message contents persistently
- Transmit call logs
- Share user information with third parties

The built-in browser uses Android's system WebView and standard network stack for normal web browsing functionality.

---

🔒 Android Sandbox & Permissions

Bluebird operates within Android's normal application sandbox and permission model.

Sensitive permissions such as:

- SMS
- Contacts
- Call Log
- Notifications
- Storage
- Camera

are only used for features explicitly related to launcher functionality, including:

- Phone and messaging integration
- Contact display
- Notification access
- Profile image selection
- File management features

Users remain in control of runtime permission grants and may revoke permissions at any time through Android system settings.

---

⚠️ Security Disclaimer

Bluebird is an open-source project provided on an "as is" basis without guarantees of security, privacy, or fitness for any particular purpose.

While reasonable efforts are made to improve application security, users should evaluate the software carefully before using it in sensitive environments.

---

🙏 Responsible Disclosure

Please allow reasonable time for vulnerabilities to be investigated and addressed before publicly disclosing security issues.

Responsible disclosure helps protect all Bluebird users.