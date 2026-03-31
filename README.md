# Chat Delay Mod (Fabric)

A client-side Fabric mod that helps you avoid self-spam.

## Features
- Blocks chat messages sent faster than your configured cooldown (default: `3.0` seconds).
- Blocks immediate duplicate messages (`hi` followed by `hi`), but allows non-adjacent repeats (`hi`, `minecraft`, `hi`).
- Sends a local warning prefixed with the mod name for visibility: `◆ ChatDelayMod ◆ Remember To Refrain from Spamming.`
- Mirrors blocked-message warnings in the action bar and plays a bell sound.

## Target versions
- Minecraft: `1.21.1` through `1.21.11`.

## Build
- Java 21 required.
- `./gradlew clean build`

## GitHub Release automation
- Push a tag like `v1.0.6` to run **Release Jar** workflow and publish jars.
