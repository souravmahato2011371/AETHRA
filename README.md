# AETHRA Launcher

**Beyond the interface.**

AETHRA is an independent Android launcher for phones and tablets. Its visual language, **AETHRA FORM**, uses a dark architectural canvas, warm paper surfaces, signal orange, and electric lime.

## First version — 0.1.0

The first development build includes:

- Android Home-app integration, so AETHRA can be selected as the default Home app.
- Installed-app discovery using Android's launchable-activity query.
- App launching from the home space and app field.
- Live local clock and date.
- Search by app label or package name.
- Saved shortcuts: long-press an app to add or remove it from the home space.
- Adaptive layouts for compact phones and wide tablet screens.
- Safe-area handling for status bars, navigation bars, and display cutouts.
- Local preferences only. No account, analytics SDK, or network permission is included.

## Not included yet

Widgets, folders, icon packs, wallpaper selection, gesture customisation, notification badges, and backup/restore are not claimed as working features. They are candidates for later milestones after the core launcher is tested on real devices.

## Build

The project uses Kotlin, Jetpack Compose, Android Gradle Plugin 9.5, and Gradle 9.8.2.

GitHub Actions builds a debug APK on pushes to `aethra-dev` and `main`. Open the repository's **Actions** tab, select **AETHRA Android Build**, open the latest successful run, and download the `aethra-debug-apk` artifact.

The workflow intentionally uses a cloud Gradle installation, so a Gradle wrapper binary is not required in the repository.

## Install and select AETHRA

1. Download and extract the APK artifact.
2. Install the APK on the Android device.
3. Press the device's Home button or use the Home gesture.
4. Select **AETHRA** in Android's Home-app chooser and choose it as the default.
5. Open the app field to search and launch installed apps.

The first build is a **debug/testing APK**, not a Play Store release. It is not signed with a permanent release key.

## Development branches

- `main` — stable project landing point.
- `aethra-dev` — active implementation and testing.

Keep day-to-day changes on `aethra-dev`. Promote a tested milestone to `main) only after it works on the target phone and tablet.

## Package

`com.aethra.launcher`

This application ID is the project's initial package identity. Keep it unchanged after distributing builds unless you intentionally want Android to treat the app as a different application.

## Brand

- Product: AETHRA Launcher
- Design language: AETHRA FORM
- Tagline: Beyond the interface.

The brand name and logo are working project assets; trademark, domain, and store-name clearance remain separate checks before a public commercial launch.
