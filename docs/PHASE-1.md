# Phase 1 — ARCore camera and depth probe

## Included
- Kotlin Android application shell.
- Camera permission and ARCore session lifecycle.
- Automatic Depth mode when supported by the runtime/device.
- Camera background preview rendered with OpenGL ES.
- Periodic raw-depth sampling and basic valid-sample counter.
- GitHub Actions debug APK build and artifact upload.

## Known limitations
- This is a sensor/runtime probe, not yet a room reconstruction engine.
- No spatial fusion, voxel map, mesh generation, scan export, or AI inference is implemented in Phase 1.
- The camera background texture mapping is intentionally minimal; orientation/aspect-ratio handling needs device validation.
- Depth sample count is a diagnostic sample, not a coverage percentage or accuracy measurement.
- ARCore and Google Play Services for AR must be available and compatible on the device.
- The app requests no network permission and does not upload camera frames.

## Device test checklist
1. Install the debug APK on a supported ARCore device.
2. Grant camera permission.
3. Confirm the live camera image appears.
4. Move the phone slowly through a textured room.
5. Confirm raw-depth dimensions and valid-sample count appear.
6. Test low-texture and dim scenes; verify the UI does not claim full coverage.
7. Repeat with network disabled after ARCore has been provisioned.
8. Record FPS, temperature/thermal status, memory, and reference-distance error before setting performance claims.
