---
name: android-build
description: Build, install, and run the GrowGuide Android app via Gradle. Supports assemble, install, and connectedCheck tasks.
disable-model-invocation: true
---

# Android Build Skill

Use this skill to compile and deploy the GrowGuide Android app.

## Available Gradle Tasks

- `assembleDebug` — Build debug APK
- `assembleRelease` — Build release APK (requires signing config)
- `installDebug` — Build and install debug APK on connected device/emulator
- `connectedCheck` — Run instrumented tests on connected device
- `compileDebugKotlin` — Compile Kotlin sources only (fast check)
- `clean` — Clean build artifacts

## Usage Examples

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Install on default device
./gradlew :app:installDebug

# Compile check (fast)
./gradlew :app:compileDebugKotlin --quiet

# Run with specific device
adb devices  # list devices first
ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDebug
```

## Prerequisites

- Android SDK installed
- `local.properties` present with `sdk.dir` (gitignored, exists locally)
- For install tasks: device connected or emulator running
