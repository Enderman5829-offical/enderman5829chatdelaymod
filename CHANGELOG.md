# Changelog

## 1.0.8 - 2026-03-31
- Added new command `chatdelay` with subcommands: `delay`, `duplicate`, and `color`.
- Ensures warning color is normalized and defaults to `red` when config color is invalid.
- Added initialization log for config values.

## 1.0.7 - 2026-03-31
- Added a GitHub Actions **Release Jar** workflow that builds with Java 21 and publishes `build/libs/*.jar` to GitHub Releases on version tags.
- Added README release automation docs for tag-based and manual release publishing from GitHub Actions.
- Added a GitHub Actions **Release Jar** workflow that builds with Java 21 and publishes `build/libs/*.jar` to GitHub Releases on version tags.
- Added README release automation docs for tag-based and manual release publishing from GitHub Actions.

## 1.0.5 - 2026-03-31
- Release follow-up: reuse the same warning text object for chat + action bar dispatch to keep formatting consistent while avoiding redundant reconstruction.
