# Chat Delay Mod (Fabric)

A client-side Fabric mod that helps you avoid self-spam.

## Features
- Blocks chat messages sent faster than your configured cooldown (default: `3.0` seconds).
- Blocks immediate duplicate messages (`hi` followed by `hi`), but allows non-adjacent repeats (`hi`, `minecraft`, `hi`).
- Sends a local warning prefixed with the mod name for visibility: `◆ ChatDelayMod ◆ Remember To Refrain from Spamming.`
- Mirrors blocked-message warnings in the action bar and plays a bell sound.

## Target versions
- Minecraft: `26.1.2`.
- Fabric Loader: `0.19.2` or newer.
- Java: `25` or newer.

## Build
- Java 25 is required for Minecraft 26.1.2.
- `./gradlew clean build`
- Runtime jar output: `build/libs/chatdelaymod-<version>.jar`.
- Optional clean export: `./scripts/export-jar.sh`, which copies the runtime jar to `build/download/chatdelaymod-<version>-mc26.1.2.jar` and prints SHA-256.

## GitHub Release automation
- Push a tag like `v1.10` to run **Release Jar** workflow and publish jars.
- The release uploads the runtime jar, sources jar, and exported `mc26.1.2` jar.

## Remote/setup helper
- Configure your remote and verify push permissions:
  - `./setup_github_remote.sh`
- Required env for setup:
  - `GITHUB_REPOSITORY=owner/repo` (or `GITHUB_REMOTE_URL=https://github.com/owner/repo.git`)
  - optional auth check token: `GITHUB_TOKEN` or `GH_TOKEN`

## CLI release helper
If you want to publish directly from your shell (without waiting for Actions):
1. Build jars: `./gradlew clean build`
2. Configure repo/token env vars.
3. Run: `./scripts/create-github-release.sh --tag v1.10`

This script pushes the tag, creates or reuses a GitHub release, uploads the Fabric runtime jar (`build/libs/*.jar`, excluding `*-sources.jar`), the sources jar when present, and the exported `mc26.1.2` jar, then verifies all expected assets exist on the release.
