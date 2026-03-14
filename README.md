# OpenParsec

OpenParsec is a simple, open-source Parsec client written using the Parsec SDK.

## Platforms

### Android (Kotlin + Jetpack Compose)

The Android app is located in the root project directory and built with Gradle.

**Requirements:**
- Android SDK 34+
- JDK 17
- Gradle 8.7+

**Building:**
```bash
./gradlew assembleDebug
```

Before building, the Parsec SDK Android native library (.so files) needs to be placed in `app/src/main/jniLibs/` for full SDK functionality. The current implementation provides the full app architecture with stub SDK methods that can be connected to the native Parsec SDK via JNI.

**Project Structure:**
```
app/src/main/java/com/aigch/openparsec/
├── MainActivity.kt              # Entry point
├── OpenParsecApp.kt             # Application class
├── ui/
│   ├── screens/                 # Compose UI screens
│   │   ├── ContentView.kt      # Navigation controller
│   │   ├── LoginScreen.kt      # Authentication
│   │   ├── MainScreen.kt       # Host/friend lists
│   │   ├── ParsecScreen.kt     # Streaming view
│   │   └── SettingsScreen.kt   # App settings
│   └── theme/                   # Material3 theming
├── parsec/                      # Parsec SDK bridge
│   ├── CParsec.kt               # Static SDK facade
│   ├── ParsecSDKBridge.kt       # SDK implementation
│   ├── ParsecTypes.kt           # Protocol/type definitions
│   ├── ParsecUserData.kt        # User data structures
│   ├── CursorPositionHelper.kt  # Coordinate conversion
│   └── DataManager.kt           # Runtime state
├── input/                       # Input handling
│   ├── KeyCodeTranslators.kt    # Keycode mapping
│   ├── GameControllerHandler.kt # Gamepad support
│   └── TouchHandler.kt          # Touch input
├── audio/
│   └── AudioPlayer.kt           # AudioTrack playback
├── network/
│   ├── NetworkHandler.kt        # API data models
│   ├── ApiClient.kt             # HTTP client
│   └── SessionStore.kt          # Session persistence
└── settings/
    └── SettingsHandler.kt        # SharedPreferences settings
```

### iOS (Swift + SwiftUI) — Reference

The original iOS app source has been preserved in the `ios/` directory for reference.

Before building the iOS app, make sure you have the Parsec SDK framework symlinked or copied to `ios/Frameworks`. Builds were tested on Xcode Version 12.5.

## Features

- **Login** — Parsec API authentication with 2FA support
- **Host Discovery** — Browse available remote computers
- **Friends** — View friends list
- **Streaming** — Remote desktop/game streaming via Parsec SDK
- **Input** — Touch controls (touchpad/direct), keyboard, mouse, and gamepad support
- **Settings** — Resolution, bitrate, decoder, cursor sensitivity, FPS preferences
- **Overlay** — In-stream controls for resolution/bitrate/display switching

## Touch Controls

You can set the touch mode in settings. Touchpad mode and direct touch mode are supported.

When streaming, you can tap with 3 fingers to bring up the on-screen keyboard.

You can toggle scrolling vs zoom behavior in the overlay menu.

## Mouse & Keyboard

USB mouse & keyboard are supported.

## Game Controllers

When streaming, press any trigger button on your controller and Parsec will recognize it. Make sure to configure the host properly (install virtual USB driver etc.) before using game controllers.

## Downloads

### iOS
<a href="https://celloserenity.github.io/altdirect/?url=https://github.com/hugeBlack/OpenParsec/releases/download/nightly/altstore.json" target="_blank">
   <img src="https://github.com/CelloSerenity/altdirect/blob/main/assets/png/AltSource_Blue.png?raw=true" alt="Add AltSource" width="200">
</a>
<a href="https://github.com/hugeBlack/OpenParsec/releases/download/nightly/OpenParsec.ipa" target="_blank">
   <img src="https://github.com/CelloSerenity/altdirect/blob/main/assets/png/Download_Blue.png?raw=true" alt="Download .ipa" width="200">
</a>