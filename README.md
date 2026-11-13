# macOS Desktop Simulation 27

**A macOS-inspired desktop environment for Android phones and tablets.**

macOS Desktop Simulation 27 recreates the feel and workflow of a desktop operating system on Android. It is an Android application and launcher, not Apple's macOS operating system or a virtual machine.

## Experience

- Desktop workspace with a custom landscape wallpaper
- macOS-inspired menu bar, Dock, Launchpad and Spotlight-style search
- Mission Control-style overview and four swipe-switchable workspaces
- Resizable-feeling desktop window controls: focus, drag, minimize, maximize and close
- Finder-style access through Android's system document picker
- Notes with local persistence
- Calendar, Photos, Mail, Maps, Clock, Contacts and Reminders surfaces
- Android app launcher that discovers and launches installed apps
- Control Center-style quick panel and notification overview
- Tablet-first responsive layout with a compact phone layout
- Immersive full-screen presentation with Android system bars available by swipe

## Platform boundaries

This project simulates the desktop experience. Android remains the underlying operating system.

- It does not run Apple's macOS kernel, frameworks or desktop binaries.
- It does not replace Android's secure lock screen or system permissions.
- Finder-style file access uses Android's document picker.
- Web pages are handed to the installed Android browser.
- Apple-only services such as iCloud, AirDrop, FaceTime, Apple ID and the Mac App Store are not implemented.
- The demo Terminal is non-executing and cannot run shell commands.
- Control Center preview switches do not silently change protected Android system settings; real settings open Android's own settings screens.

## Android implementation

- Kotlin
- Jetpack Compose
- AndroidX
- Android intents and system document/photo pickers

## Build

GitHub Actions runs Android lint/resource verification and assembles a debug APK on pushes to `main`. Successful main-branch builds publish the APK to GitHub Releases.

The debug APK is for testing and is not a Play Store production-signed release.

## Application identity

The visible app name is **macOS Desktop Simulation 27**.

The existing Android application ID `com.aethra.launcher` is intentionally retained so an installed copy can be updated without changing its package identity. This technical identifier does not appear as the product name in the interface.

## Licensing

See `LICENSE` and `THIRD_PARTY_NOTICES` for project and third-party licensing information.
