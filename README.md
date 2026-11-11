# AETHRA Music

**AETHRA — Music Without Limits.**

AETHRA is an Android music player focused on local playback and frictionless online music discovery.

## Product principles

- No AETHRA account or sign-in.
- Local music playback using Android MediaStore and Media3.
- YouTube music-video discovery through the supported YouTube Data API v3.
- YouTube search results open on YouTube for listening.
- No AETHRA-inserted advertisements.

## Online discovery

AETHRA uses the official YouTube Data API v3 search endpoint for video metadata, titles, channel names, and thumbnails. It does not use the zerodytrash internal-client discovery script as an API: that repository is a research tool for undocumented client identifiers, not a supported API or Android SDK.

The YouTube Data API does not return direct audio-stream URLs and does not grant rights to redistribute or stream commercial recordings inside AETHRA. Search results therefore open through YouTube rather than being falsely presented as direct in-app audio streams.

The API key is injected at build time from the `YOUTUBE_API_KEY` Gradle property or environment variable. For GitHub Actions builds, configure a repository Actions secret named `YOUTUBE_API_KEY`. Restrict the Google key to the YouTube Data API and appropriate application/API usage. A key embedded in an APK is extractable; build-time injection is not a substitute for a backend proxy or proper key restrictions.

## Android implementation

- Kotlin
- Jetpack Compose
- AndroidX Media3 ExoPlayer and MediaSession
- YouTube Data API v3 for discovery
- Android MediaStore for local audio discovery

## Build

GitHub Actions runs Android lint/resource verification and assembles a debug APK on pushes to `main`. Successful main-branch builds publish the APK to GitHub Releases.

The debug APK is for testing and is not a Play Store production-signed release.

## Package

`com.aethra.launcher`

The application ID is retained to preserve update compatibility with existing installations.

## Licensing

See `LICENSE` and `THIRD_PARTY_NOTICES` for project and third-party licensing information.
