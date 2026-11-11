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

## Online music integration

The current `main` branch uses the Internet Archive as its only online catalogue. The `integration/rimusic-streaming-core` branch is the isolated GPL-3.0 integration track for evaluating and porting selected RiMusic streaming components. RiMusic is archived/closed, so its upstream implementation must be audited and maintained before it can be treated as a reliable provider.

Any RiMusic-derived source in this branch remains subject to GPL-3.0. See `LICENSE` and `THIRD_PARTY_NOTICES`. The AETHRA interface, brand, and product experience remain independently designed.

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

## Licensing

This integration branch is licensed under GPL-3.0 to support incorporation of compatible RiMusic-derived components. Any distributed build containing those components must comply with the GPL-3.0, including providing the corresponding source code and preserving applicable notices. This is a project engineering note, not legal advice.

## Roadmap

The next engineering priorities are integrating and validating the RiMusic-derived catalogue/streaming layer, preserving local playback, strengthening playback and queue behaviour, adding persistent playlists and listening history, improving metadata/artwork reliability, and testing performance across low- and high-end Android devices. Features will only be marked complete after implementation and verification.
