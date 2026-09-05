# Remote (Bluebird Team) Notifications

This document explains the system that lets the Bluebird team push
announcements, update notices, and warnings to every install of the app —
without a backend server, without push notification infrastructure, and
without any background polling.

It works by publishing a single JSON file (`notify.json`) to a GitHub repo.
The app periodically checks that file, turns new entries into Action Center
items, and toasts the ones it hasn't shown before.

---

## Table of Contents

- [How it works, in one paragraph](#how-it-works-in-one-paragraph)
- [Files involved](#files-involved)
- [The `notify.json` schema](#the-notifyjson-schema)
- [Publishing a new notification](#publishing-a-new-notification)
- [When a fetch actually happens](#when-a-fetch-actually-happens)
- [What happens during a fetch](#what-happens-during-a-fetch)
- [Deduplication: why content, not id](#deduplication-why-content-not-id)
- [Expiry](#expiry)
- [Persisted state (SharedPreferences)](#persisted-state-sharedpreferences)
- [Action Center vs. Toasts](#action-center-vs-toasts)
- [Testing / forcing a fetch during development](#testing--forcing-a-fetch-during-development)
- [Troubleshooting](#troubleshooting)
- [Extending this system](#extending-this-system)

---

## How it works, in one paragraph

The Bluebird team edits a JSON file at a fixed GitHub raw URL. The app has no
backend of its own for this — that URL *is* the backend. `LauncherViewModel`
exposes `refreshRemoteNotificationsIfDue()`, which two places in the app call
whenever the user is actively using it (launching the app, opening Action
Center). That function only actually reaches the network if it's been more
than **2 hours** since the last attempt; otherwise it's a no-op. When it does
fetch, it parses the manifest, drops anything expired, updates Action
Center's list, and fires a toast for any notification whose *content* (not
just its id) hasn't been shown before.

---

## Files involved

| File | Role |
|---|---|
| `LauncherViewModel.kt` | Owns all the logic: fetching, throttling, parsing, deduplication, expiry, persistence. Search for the `// ─── Remote (Bluebird team) announcements` block. |
| `MainActivity.kt` | Triggers a throttled refresh on app launch (`onCreate`/first compose) and on `onResume()`. |
| `ui/components/ActionCenter.kt` | Triggers a throttled refresh when the Action Center panel opens. Also renders the notification list. Defines `BluebirdRemoteNotification`. |
| `ui/components/NotificationToast.kt` | Defines `ToastNotifData` and the toast UI. `BluebirdRemoteNotification.toToastData()` maps a remote notification into a toast. |
| `notify.json` (hosted on GitHub, **not** in this repo's build) | The actual content published by the team. See schema below. |

---

## The `notify.json` schema

Hosted at:

```
https://raw.githubusercontent.com/Norbert-web/bluebird-releases/main/assets/bluebird/notify.json
```

(This URL is a `private const val REMOTE_NOTIF_URL` at the top of
`LauncherViewModel.kt` if it ever needs to change.)

```json
{
  "schema_version": 1,
  "notifications": [
    {
      "id": "bluebird-2026-008",
      "type": "update",
      "priority": "high",
      "title": "Bluebird v1.8 Is Here! 🚀",
      "body": "The long-awaited Bluebird v1.8 is officially released! ...",
      "timestamp": "2026-08-24T00:00:00Z",
      "expires_at": null,
      "icon": "update",
      "action": {
        "label": "Get v1.8",
        "url": "https://bluebirduganda.dpdns.org"
      },
      "badge_color": "#107C10"
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `id` | **Yes** | Any unique string. Used for Action Center list identity and dismissal. **Not** used for toast dedup — see [Deduplication](#deduplication-why-content-not-id) below, so it's safe to change an id without worrying about re-triggering a toast for unchanged content. |
| `type` | No (default `"announcement"`) | Free-form (`"announcement"`, `"update"`, `"warning"`, etc.). Currently cosmetic/informational — not branched on in code today. |
| `priority` | No (default `"normal"`) | Free-form (`"normal"`, `"high"`). Currently cosmetic — not branched on in code today. |
| `title` | **Yes** | Shown in Action Center and in the toast. |
| `body` | **Yes** | Shown in Action Center and in the toast (2-line max in the toast, full text in Action Center). |
| `timestamp` | No (default `""`) | Informational display timestamp. **Not** used for dedup or expiry — bumping it does not make an unchanged notification re-toast, and does not affect ordering logic. |
| `expires_at` | No (default `null`) | ISO-8601 instant (e.g. `"2026-09-01T00:00:00Z"`). If in the past at fetch time, the entry is dropped entirely — never shown, never toasted. `null` = never expires. |
| `icon` | No | Present in the JSON but **not currently read** by the app (no field for it on `BluebirdRemoteNotification`). Reserved for future use. |
| `action` | No (default `null`) | Object with `label` and `url`. If present, renders as a button in Action Center / the toast that opens `url` when tapped. Omit or set `null` for a plain announcement with no CTA. |
| `badge_color` | No (default `"#0078D4"`) | Hex color (`#RRGGBB`), used as the accent color for the notification's icon chip and toast accent. Falls back to the default blue if it fails to parse. |

`schema_version` at the top level is not currently read by the app — it's
there for future-you if the shape ever needs a breaking change.

---

## Publishing a new notification

1. Add a new object to the `notifications` array in `notify.json`, with a
   fresh `id`.
2. Commit/push to the `bluebird-releases` repo, `main` branch, at
   `assets/bluebird/notify.json`.
3. That's it — no app update, no build, no release needed. Users will pick
   it up the next time their app fetches (see [When a fetch actually
   happens](#when-a-fetch-actually-happens)) — worst case, within 2 hours of
   their next app open.

**To retire a notification** without waiting for someone to dismiss it,
either delete it from the array or give it a `expires_at` in the past — both
remove it from every user's Action Center on their next fetch.

**To edit a notification that's already gone out** (fix a typo, change a
link): just edit its `title`/`body`/`action`/etc. in place, keeping the same
`id` — this is treated as a **content change**, so it *will* toast again for
users who already saw the old version (their old content hash won't match
the new one). If you want to correct something silently (no re-toast), you
technically can't with the current system — the trade-off is deliberate,
see the section below.

---

## When a fetch actually happens

There is **no background timer, no `WorkManager` job, no service**. A fetch
only happens when the user is demonstrably using the app, at two call sites:

| Trigger | Where |
|---|---|
| App launch (cold start / first compose) | `MainActivity.kt`, inside `setContent { }` |
| App resumed (brought back to foreground) | `MainActivity.onResume()` |
| Action Center opened | `ActionCenter.kt`, `LaunchedEffect(Unit)` at the top of the composable |

All three call the same function: `viewModel.refreshRemoteNotificationsIfDue()`.

That function checks a persisted timestamp (`lastRemoteFetchAt`) and only
proceeds if **more than 2 hours** have passed since the last attempt
(`REMOTE_NOTIF_MIN_INTERVAL_MS` in `LauncherViewModel.kt`). Otherwise it's a
no-op. This means:

- Calling it from three different places is safe — none of them need to
  coordinate with each other. Whichever one fires first in a given 2-hour
  window does the real work; the rest are free.
- A user who opens the app 20 times in an hour still makes at most one real
  network request in that hour.
- A user who never reopens the app never causes any network traffic at all
  — the "poll" only exists while the app is actually being used.
- A `remoteFetchInFlight` flag additionally guards against two triggers
  firing at almost the same instant (e.g. cold launch immediately followed
  by opening Action Center) both starting an overlapping fetch.

There's also `refreshRemoteNotifications()` (no `IfDue`) — this **forces**
an immediate fetch, ignoring the throttle. It isn't called from anywhere
automatically; it exists as a hook for an explicit "Refresh" button, if one
ever gets added to Action Center.

To change the interval, edit `REMOTE_NOTIF_MIN_INTERVAL_MS` at the top of
`LauncherViewModel.kt`:

```kotlin
private const val REMOTE_NOTIF_MIN_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 hours
```

---

## What happens during a fetch

Inside `fetchRemoteNotificationsOnce()`:

1. `GET` the raw JSON over a plain blocking `URL(...).readText()` call, on
   `Dispatchers.IO`.
2. As soon as the read succeeds, `lastRemoteFetchAt` is updated — **before**
   parsing. This is intentional: a malformed manifest shouldn't cause the
   app to retry on every single app open. If the *network call itself*
   fails (no connectivity, timeout, DNS failure), the timestamp is left
   untouched, so the very next due-check will simply try again rather than
   waiting out a full interval on a failed attempt.
3. Parse the `notifications` array into `BluebirdRemoteNotification`
   objects.
4. Drop any entry whose `expires_at` is in the past (see
   [Expiry](#expiry)).
5. Compute a content hash for each surviving entry (see
   [Deduplication](#deduplication-why-content-not-id)).
6. Update `uiState.remoteNotifications` with the full surviving list — this
   is what Action Center renders.
7. For every entry whose content hash hasn't been seen before, **and** only
   if this isn't the very first fetch the app has ever done, **and** only if
   `bannersAllowed()` — emit a toast.
8. Persist the updated set of seen content hashes and seen ids.

Any exception anywhere in this (bad JSON, missing required field, etc.) is
caught, logged via `printStackTrace()`, and swallowed — a broken manifest
should never crash the app or spam a user with error UI.

---

## Deduplication: why content, not id

Two separate persisted sets, doing two separate jobs:

- **`seenRemoteIds`** — every `id` the app has ever fetched. Its *only* job
  is answering "is this the first fetch this install has ever done?" (via
  `remoteBootstrapped`). That check exists so a fresh install doesn't toast
  every historical announcement at once the first time it fetches — it
  silently populates Action Center instead.

- **`seenRemoteContentHashes`** — a SHA-256 hash of each notification's
  *meaningful content* (`type`, `priority`, `title`, `body`, `actionLabel`,
  `actionUrl`, `badgeColor` — see `contentSignatureHash()`), **deliberately
  excluding `id` and `timestamp`**. A toast only fires if this hash hasn't
  been seen. This means:

  - Republishing an unchanged announcement under a brand-new `id` does
    **not** cause a duplicate toast.
  - Just bumping `timestamp` on an unchanged announcement does **not**
    cause a duplicate toast.
  - Actually changing the title/body/action/color of an existing `id`
    **does** count as new content, and **will** toast again — the system
    can't tell the difference between "a genuinely new notification" and
    "an edited one," and treats both as worth surfacing.

If you ever need "edit silently, never re-toast," that would require a
different signal — e.g. hashing on `id` alone instead of content, or adding
an explicit `"silent": true` field to the schema and checking it before
emitting a toast. Not implemented today.

---

## Expiry

`expires_at` is an ISO-8601 instant string. During parsing, each entry is
checked against `java.time.Instant.now()`:

```kotlin
val isExpired = notif.expiresAt?.let { expiresAt ->
    try { java.time.Instant.parse(expiresAt).isBefore(now) } catch (_: Exception) { false }
} ?: false
```

- `expires_at: null` (or omitted) → never expires.
- A malformed date string is treated as **not expired** (fails open, so a
  typo in the manifest doesn't accidentally hide a live notification).
- Expired entries are filtered out **before** they ever reach `uiState` —
  they don't appear in Action Center at all, and don't need to be
  dismissed. This is different from `dismissedRemoteNotificationIds`
  (below), which is for notifications a user has manually closed.

---

## Persisted state (SharedPreferences)

All keys live in the same `SharedPreferences` file the rest of
`LauncherViewModel` uses (`launcher_prefs_v3`). Relevant keys for this
system:

| Key | Type | Purpose |
|---|---|---|
| `remote_notif_last_fetch_at` | `Long` | Epoch ms of the last successful network read. Drives the 2h throttle. |
| `remote_notif_bootstrapped` | `Boolean` | Whether the very first-ever fetch has completed. Suppresses toasts on that first fetch. |
| `seen_remote_notif_ids` | `Set<String>` | All ids ever fetched. Only used to compute `remoteBootstrapped`'s effect — not used for dedup. |
| `seen_remote_notif_hashes` | `Set<String>` | Content hashes of everything ever toasted. Drives dedup — see above. |

Separately, `dismissedRemoteNotificationIds` lives in `uiState`
(`LauncherUiState`, not raw SharedPreferences here) and is what
`dismissRemoteNotification(id)` writes to — that's the "user tapped the X
on this card in Action Center" state, independent of everything above.

To fully reset this system on a device for testing, clear the app's data,
or manually strip those four keys from `launcher_prefs_v3` via
`adb shell run-as <package> ...` (rooted/debug builds) or
`pm clear io.github.norbertweb.bluebird` (nukes all app state, not just
this).

---

## Action Center vs. Toasts

These are two independent surfaces reading the *same* fetched list, kept in
sync by both reading `uiState.remoteNotifications`:

- **Action Center** (`ActionCenter.kt`) shows every non-expired,
  non-dismissed notification currently in `uiState.remoteNotifications`,
  every time it's opened — regardless of whether it's "new."
- **Toasts** (`NotificationToastHost` in `NotificationToast.kt`) only ever
  fire once per genuinely-new content hash, and only if
  `bannersAllowed()` (respecting whatever Do Not Disturb / notification
  banner settings exist elsewhere in the app) and only after the very first
  bootstrap fetch.

So: a notification can be sitting in Action Center for weeks without ever
having toasted (e.g. it arrived during the first-ever fetch), and that's
expected behavior, not a bug.

---

## Testing / forcing a fetch during development

Because of the 2h throttle, iterating on `notify.json` content while testing
can be slow. Options, roughly in order of convenience:

1. **Clear app data** (`Settings → Apps → Bluebird → Storage → Clear Data`,
   or `adb shell pm clear io.github.norbertweb.bluebird`). Wipes
   `lastRemoteFetchAt` back to 0, so the next launch fetches immediately.
   Also wipes every other app preference, so it's blunt but reliable.
2. **Temporarily call `refreshRemoteNotifications()`** (the force variant,
   no `IfDue`) instead of `refreshRemoteNotificationsIfDue()` at one of the
   call sites while testing, then revert before committing.
3. **Temporarily lower `REMOTE_NOTIF_MIN_INTERVAL_MS`** to something small
   (e.g. `10_000L` for 10 seconds) while testing, then revert.

Don't ship any of these three changes — they're dev-only workarounds for
the throttle, not toggles meant to exist in a release build.

---

## Troubleshooting

**Notifications aren't showing up at all.**
- Confirm `notify.json` is valid JSON and reachable at the raw URL in a
  browser.
- Confirm the device actually has connectivity when the app is opened (the
  fetch fails silently — check Logcat for the `printStackTrace()` output
  from `fetchRemoteNotificationsOnce()`'s catch block).
- Confirm it's actually been >2h since the last attempt, or use one of the
  [testing workarounds](#testing--forcing-a-fetch-during-development) above.

**A notification keeps re-toasting on every fetch.**
- Something about its `id` **and** content is changing between fetches —
  double check the manifest isn't being regenerated with fresh `title`/
  `body` text (even whitespace differences) each time it's published.

**An edited notification isn't re-toasting when I wanted it to.**
- Check that you actually changed one of the hashed fields
  (`type`/`priority`/`title`/`body`/`action.label`/`action.url`/
  `badge_color`). Editing only `timestamp` or `icon` is invisible to the
  dedup hash by design.

**A notification won't go away.**
- Check `expires_at` — if it's `null` or still in the future, it will keep
  showing in Action Center until a user manually dismisses it. Set
  `expires_at` to a past date to retire it for everyone immediately.

---

## Extending this system

Ideas that would build cleanly on the current structure, if ever needed:

- **Manual refresh button in Action Center** — call the existing
  `viewModel.refreshRemoteNotifications()` (the force variant already
  exists, just isn't wired to any UI).
- **Per-user targeting** (e.g. beta channel only) — would need a client
  identifier of some kind and either multiple manifest files or a `target`
  field checked during parsing; not present today.
- **Rich content** (images, multiple actions) — would mean extending both
  the schema and `BluebirdRemoteNotification`/`ToastNotifData`.
- **Silent edits** (fix a typo without re-toasting) — see the note at the
  end of the [Deduplication](#deduplication-why-content-not-id) section.
