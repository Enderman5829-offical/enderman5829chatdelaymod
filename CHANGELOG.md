# Changelog

## 1.10 - 2026-05-09
- Ported the mod build to Minecraft `26.1.2`, Fabric Loader `0.19.2`, Fabric API `0.148.0+26.1.2`, Loom `1.16`, Gradle `9.4.0`, and Java `25`.
- Migrated the client source to Minecraft 26.1 unobfuscated/Mojang names and Fabric API 26.1 command naming.
- Updated release/export helpers for 26.1.2 builds, which produce runtime jars in `build/libs` instead of `build/devlibs`.

## 1.0.7 - 2026-03-31
- Removed environment-specific secret variable references from setup docs/script to keep token handling generic (`GITHUB_TOKEN` / `GH_TOKEN`).
- Added `scripts/create-github-release.sh` to push a version tag, create a GitHub release via REST API, and upload built jar assets from the local build output.
- Fixed release helper asset selection so it always uploads the actual Fabric remapped jar (not the `-sources` jar) along with dev and clean jars.

## 1.0.6 - 2026-03-31
- Added a GitHub Actions **Release Jar** workflow that builds with Java 21 and publishes `build/libs/*.jar` to GitHub Releases on version tags.
- Added README release automation docs for tag-based and manual release publishing from GitHub Actions.

## 1.0.5 - 2026-03-31
- Release follow-up: reuse the same warning text object for chat + action bar dispatch to keep formatting consistent while avoiding redundant reconstruction.
