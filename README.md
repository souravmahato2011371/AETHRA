# AETHRA Music

**AETHRA — Music Without Limits.**

AETHRA is an Android music player built around one simple idea: open the app and play. It combines music stored on the device with discovery from public, openly licensed audio catalogues.

## Product principles

- No AETHRA account or sign-in.
- No listener-supplied API keys or provider credentials.
- Local music playback after the Android media permission is granted.
- Open-catalogue discovery with source and licence information where available.
- Background playback through AndroidX Media3.
- A unified search experience for local tracks and the currently integrated open catalogue.
- No AETHRA-inserted advertisements.

## Current online catalogue

The current online source is the Internet Archive. AETHRA searches audio items that declare a Creative Commons licence and resolves playable MP3/Ogg files when available.

This is an open catalogue, not a licensed substitute for Spotify, YouTube Music, Apple Music, or every commercial release. Search coverage, metadata, artwork, stream availability, and item-level rights vary. AETHRA does not bypass paywalls, access controls, or provider restrictions.

## Android implementation

- Kotlin
- Jetpack Compose
- AndroidX Media3 ExoPlayer and MediaSession
- Internet Archive public metadata/search endpoints
- Android MediaStore for local audio discovery

## Build

GitHub Actions runs Android lint/resource verification and assembles a debug APK on pushes to `main`. Successful main-branch builds publish the APK to GitHub Releases.

The debug APK is for testing and is not a Play Store production-signed release.

## Package

`com.aethra.launcher`

The application ID is retained to preserve update compatibility with existing installations.

## Roadmap

The next engineering priorities are expanding legitimate open-catalogue coverage, strengthening playback and queue behaviour, adding persistent playlists and listening history, improving metadata/artwork reliability, and testing performance across low- and high-end Android devices. Features will only be marked complete after implementation and verification.
