# Contributing to Bluebird

First off, thank you for considering contributing to Bluebird. This project recreates a full Windows 11 desktop experience on Android using Jetpack Compose, and there's plenty of room to help — from fixing a layout bug to building out a whole new built-in app.

This guide covers how to set up the project, the standards we follow, and how to submit changes.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Ways to Contribute](#ways-to-contribute)
- [Development Setup](#development-setup)
- [Project Conventions](#project-conventions)
- [Bug Reports](#bug-reports)
- [Feature Requests](#feature-requests)
- [Pull Requests](#pull-requests)
- [Branching Strategy](#branching-strategy)
- [Testing Your Changes](#testing-your-changes)
- [Commit Message Style](#commit-message-style)
- [Questions](#questions)

---

## Code of Conduct

Be respectful, be constructive, and assume good intent. Bluebird is a solo/small-team project built in public — disagreements about implementation are fine and expected, personal attacks are not.

---

## Ways to Contribute

You don't need to write Kotlin to help:

- **Bug reports** — even a clear description with device/Android version is valuable
- **Design feedback** — screenshots comparing Bluebird to real Windows 11 behavior are especially useful, since visual/behavioral parity is a core goal
- **Documentation** — README clarity, code comments, this file
- **Translations** — no localization system exists yet; opening an issue to discuss one is welcome
- **Code** — bug fixes, new features, performance improvements

If you're not sure whether something is worth doing, open an issue first and ask — it saves everyone time versus a PR that doesn't align with the project direction.

---

## Development Setup

### Prerequisites

- Android Studio **Hedgehog (2023.1.1)** or newer
- JDK **17** or newer
- Gradle **8.5.2** (wrapper included, no manual install needed)
- A physical Android device or emulator running **Android 8.0+ (API 26+)**

> **Note:** Some launcher-specific behavior (default home app selection, boot receiver, notification listener access) does not reliably work on emulators. Test on a real device before opening a PR touching those areas.

### Getting the code running

```bash
git clone https://github.com/norbert-web/bluebird.git
cd bluebird
```

1. Open the `Bluebird/` folder in Android Studio and let Gradle sync complete.
2. Connect a device or start an emulator (API 26+).
3. Run `./gradlew installDebug` or use the Run button.
4. When prompted, set Bluebird as your default launcher to test the full experience.

To go back to your normal launcher at any point: **Settings → Apps → Default Apps → Home App**, and pick something else.

---

## Project Conventions

- Follow standard [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Jetpack Compose best practices — hoisted state, stable parameters, small and focused composables
- **All state mutations go through `LauncherViewModel`.** Never mutate `LauncherUiState` directly from a composable — this is the single most important architectural rule in the codebase, since the entire shell relies on unidirectional data flow through one `StateFlow`
- Add `@Preview` annotations to new composables where it's useful for reviewing UI in isolation
- New icons should use **Fluent UI System Icons** to stay visually consistent with the v2.x "Windows 11 parity" direction — avoid introducing Material icons for new UI unless no Fluent equivalent exists
- Keep new built-in apps self-contained in their own package (see `wordprocessor/`, `editor/`, `browser/` for examples of the existing pattern)

---

## Bug Reports

Open an issue on [GitHub Issues](https://github.com/norbert-web/bluebird/issues) and include:

- Android version and device model (RAM matters too — Bluebird targets low-RAM devices, so this helps a lot)
- Steps to reproduce
- Expected vs. actual behavior
- Screenshot or screen recording if it's a visual bug
- Logcat output filtered by `io.github.norbertweb.bluebird`, if available

---

## Feature Requests

Check the [Roadmap in the README](README.md#roadmap) first — it may already be planned. If not, open an issue describing the feature and, ideally, how it maps to real Windows 11 behavior (since visual/behavioral fidelity to Windows 11 is the project's north star).

---

## Pull Requests

1. **Fork** the repository.
2. **Create a branch** off `develop` (not `main`): `git checkout -b feature/your-feature-name`
3. **Make your changes**, following the conventions above.
4. **Test on a real device.** Emulators can miss launcher-specific behavior — see the note under Development Setup.
5. **Commit** with a clear, descriptive message (see style below).
6. **Push** your branch and open a Pull Request against `develop`.
7. In the PR description, explain *what* changed and *why*, and include before/after screenshots for any visual change.

Small, focused PRs are much easier to review than large ones touching many unrelated areas — if a change spans multiple concerns, consider splitting it into separate PRs.

---

## Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, released code only |
| `develop` | Integration branch for the next release — **PRs should target this** |
| `feature/*` | New features |
| `fix/*` | Bug fixes |
| `release/*` | Release preparation |

---

## Testing Your Changes

```bash
./gradlew test          # Unit tests
./gradlew connectedTest # Instrumented tests (requires a connected device/emulator)
```

There isn't full test coverage across the shell yet — if you're adding a non-trivial piece of logic (especially anything in `LauncherViewModel`), tests are appreciated but not blocking. Manual verification on-device is the current baseline expectation for UI changes.

---

## Commit Message Style

Prefix commits with a type, similar to Conventional Commits:

```
feat: add window resize handles
fix: correct desktop icon scaling on tablets
docs: clarify permissions section in README
refactor: simplify recycle bin state handling
```

This isn't strictly enforced, but it makes the commit history and future changelogs easier to generate.

---

## Questions

Not sure where something belongs, or want to discuss an idea before building it? Use [GitHub Discussions](https://github.com/norbert-web/bluebird/discussions) or email **trebronwayne@gmail.com**.

Thanks again for taking the time to contribute — every issue, PR, and piece of feedback helps Bluebird get closer to the real thing.
