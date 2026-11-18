# Installation and development

The first release candidate uses version `0.1.0-preview.1` under
`io.github.apdelrahman1911.nativecmpkit`. These coordinates are pending publication.

## Components

Use the root multiplatform coordinate in `commonMain`. Gradle selects the Android,
desktop JVM, iOS device, iOS simulator, JS or Wasm artifact. See the [platform
guides](platforms/README.md) for runtime availability by feature.

| Artifact ID | Direct Kira dependencies |
| --- | --- |
| `kira-camera-picker` | `kira-io` |
| `kira-core` | None |
| `kira-cupertino-icons` | None |
| `kira-file-picker` | `kira-io` |
| `kira-file-picker-coil` | `kira-core`, `kira-io` |
| `kira-geo` | None |
| `kira-io` | `kira-core` |
| `kira-navigation` | None |
| `kira-permissions` | `kira-permissions-core`, `kira-permissions-background-location`, `kira-permissions-bluetooth`, `kira-permissions-calendar`, `kira-permissions-camera`, `kira-permissions-contacts`, `kira-permissions-gallery`, `kira-permissions-location`, `kira-permissions-microphone`, `kira-permissions-notifications`, `kira-permissions-wifi` |
| `kira-permissions-background-location` | `kira-permissions-core` |
| `kira-permissions-bluetooth` | `kira-permissions-core` |
| `kira-permissions-calendar` | `kira-permissions-core` |
| `kira-permissions-camera` | `kira-permissions-core` |
| `kira-permissions-contacts` | `kira-permissions-core` |
| `kira-permissions-core` | None |
| `kira-permissions-gallery` | `kira-permissions-core` |
| `kira-permissions-location` | `kira-permissions-core` |
| `kira-permissions-microphone` | `kira-permissions-core` |
| `kira-permissions-notifications` | `kira-permissions-core` |
| `kira-permissions-wifi` | `kira-permissions-core` |
| `kira-share` | `kira-io` |
| `kira-ui` | `kira-core` |
| `kira-webview` | None |

`kira-permissions` is an optional bundle of permission components. For a single
permission, choose its leaf artifact. No toolkit-wide bundle is required.

For example, after publication, add `mavenCentral()` and declare:

```kotlin
implementation("io.github.apdelrahman1911.nativecmpkit:kira-ui:0.1.0-preview.1")
implementation("io.github.apdelrahman1911.nativecmpkit:kira-permissions-camera:0.1.0-preview.1")
```

Desktop WebView applications supply the JavaFX runtime for the destination OS
and architecture. See the [desktop setup](platforms/desktop.md#javafx-specific-prerequisites).
Navigation and Geo keep their existing experimental APIs and platform limitations.
Maps, media and notification scheduling are prototypes without a published component.

## Migrate existing source integrations

Replace the old module coordinates with `kira-*` and update imports to
`io.github.apdelrahman1911.kira.*`. Opt-in names use `Kira`, such as
`ExperimentalKiraUiApi`. Regenerate any Compose resource imports and rebuild
applications and iOS frameworks that used the old package. Old binary clients
require their original artifacts or source pins until rebuilt. Existing Git
commits and tags retain their original identities.

## Build from source

```sh
git clone https://github.com/Apdelrahman1911/NativeCmpKit.git
cd NativeCmpKit
./gradlew :kira-io:desktopTest :kira-file-picker:desktopTest
```

Use JDK 17, Android SDK package `platforms;android-37.0` and the checked-in
Gradle wrapper. Desktop native builds require Rust from `rust-toolchain.toml`.
On Linux the picker requires `pkg-config` and Wayland development metadata.
Apple WebView compilation requires macOS and Xcode.
