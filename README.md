# AI-FUSION Halo Vision

Android-first, offline-oriented room scanning prototype.

## Phase 1: ARCore camera and depth probe

This repository currently contains the first Android/Kotlin implementation:
- ARCore session lifecycle and camera permission handling.
- Automatic depth mode when ARCore reports it as supported.
- OpenGL ES camera background.
- Periodic Raw Depth API probe with a diagnostic count of sampled non-zero depth pixels.
- GitHub Actions workflow that builds and uploads a debug APK artifact.

### Build locally

Requirements: JDK 17, Android SDK, and Gradle 8.9.

```bash
gradle --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions

Push to `main`, open a pull request, or run **Actions → Android Build → Run workflow**. On success, download the artifact named `AI-FUSION-Halo-Vision-debug` from the workflow run.

## Important limitations

- This is a sensor/runtime probe, not yet a complete room scanner.
- Spatial fusion, voxel mapping, mesh reconstruction, scan saving/export, object detection, and AI inference are not implemented yet.
- The raw-depth sample count is diagnostic only; it is not a depth-coverage percentage or an accuracy claim.
- ARCore and Google Play Services for AR must be supported and provisioned on the device.
- Test the APK on the target phone before treating camera orientation, depth availability, performance, or offline operation as confirmed.
- The app does not request network access or upload camera frames.

See [Phase 1 notes](docs/PHASE-1.md) for validation steps.
