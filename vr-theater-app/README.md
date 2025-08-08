# VR Theater

A mobile VR theater app that mirrors any running Android game's screen into a stereoscopic virtual theater overlay for phone-slot VR headsets (Cardboard/Daydream-style). Works on Android 10–14.

## Environment Setup (Linux)
Run once to provision JDK, SDK, NDK, CMake, emulator, and Android Studio:
```bash
bash /workspace/android_setup.sh
```
This accepts licenses and creates AVD `Pixel_7_API_34`.

## Build
1. Generate Gradle wrapper and assemble:
   ```bash
   cd /workspace/vr-theater-app
   gradle wrapper --gradle-version 8.7
   ./gradlew assembleDebug
   ```
2. Install APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Use
- Open app, grant overlay and capture permissions.
- Start Projection, pick a game from the list, then use a VR viewer.
- Adjust Calibration in Settings for your optics.

## Controllers
- Tested with standard HID Bluetooth and USB gamepads (A/B/X/Y, triggers, D-pad). Input passes through to the foreground game since overlay is non-focusable.

## CI Cache
- `$HOME/.gradle/caches`, `$HOME/.gradle/wrapper`
- `/opt/android-sdk`

## License
See `../LICENSE`.