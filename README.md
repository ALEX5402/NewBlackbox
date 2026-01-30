# BlackBox - Virtual Engine

<p align="center">
  <img src="assets/usage.gif" alt="BlackBox Banner" width="100%"/>
</p>

BlackBox is a virtual engine that allows you to clone and run virtual applications on Android devices without installing APKs. This project works on Android 5.0 to 14.0+ and supports multiple architectures (ARM64, ARMv7, x86).

## Overview

This enhanced edition includes bug fixes, stability improvements, and Android 14+ compatibility tailored for modern devices.

### Key Features

*   **Virtual App Cloning**: Run multiple instances of applications.
*   **Sandboxed Environment**: Isolated process execution.
*   **No Root Required**: Runs entirely in userspace.
*   **Multi-Architecture**: Support for 32-bit and 64-bit apps.
*   **Device Spoofing**: Modify device information for virtual apps.
*   **Fake Location**: Spoof GPS coordinates.

## Requirements

*   **Android Version**: Android 5.0 (API 21) or higher.
*   **RAM**: 2GB minimum recommended.
*   **Architecture**: ARMv7a, ARM64-v8a, x86.

## Build Instructions

### Prerequisites
*   Android Studio (Arctic Fox or newer)
*   JDK 17
*   Android SDK 34+
*   NDK (Version 29.0.13846066)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/your-repo/NewBlackbox.git
cd NewBlackbox

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

## Integration

To use BlackBox Core in your own project, add the AAR dependency:

```gradle
dependencies {
    implementation fileTree(dir: "libs", include: ["*.aar"])
}
```

Refer to `Docs.md` for detailed API documentation.

## Troubleshooting

*   **App Crashes**: Check logcat for UID mismatches or permission errors.
*   **Installation Failures**: Verify potential architecture mismatches or storage permissions.
*   **Android 15**: Ensure you are using the latest build which handles stricter security policies.

## Credits

*   **Main Developer**: ALEX502
*   **Original Framework**: VirtualApp, VirtualAPK
*   **Native Hooks**: Dobby, xDL
*   **Reflection**: BlackReflection, FreeReflection

## License

Copyright 2022 BlackBox

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
