# NativeCmpKit

NativeCmpKit is a Kotlin Multiplatform toolkit maintained by **Abdelrahman Fahmy**.
Its Kira modules cover adaptive UI, permissions, files, camera, sharing,
navigation, location, icons and WebView. Each component has its own Maven coordinate.

## Installation

The first Maven Central release is in verification. The
[coordinate list](docs/installation.md) identifies the planned artifacts and what
each component pulls in. Choose the components your application uses.

- Maven group `io.github.apdelrahman1911.nativecmpkit`
- Artifact IDs `kira-*`
- Kotlin packages `io.github.apdelrahman1911.kira.*`

The Gradle build declares Android, desktop JVM, iOS arm64 device and simulator,
browser JavaScript and Wasm targets. Feature support varies by module and platform.
See the [platform guides](docs/platforms/README.md).

## Build from source

```sh
git clone https://github.com/Apdelrahman1911/NativeCmpKit.git
cd NativeCmpKit
./gradlew :kira-io:desktopTest :kira-file-picker:desktopTest
```

Use JDK 17, Android SDK 37 and the checked-in Gradle wrapper. Desktop file picker
and share builds need the pinned Rust toolchain and their host dependencies. Apple
WebView builds require macOS and Xcode. See [development setup](docs/installation.md).

## Development and migration

- [Testing and evidence](docs/testing/README.md)
- [Architecture and component contracts](docs/architecture/README.md)
- [Audit and roadmap](docs/roadmap/README.md)
- [Preserved Git history and metadata](docs/migration/README.md)
- [Repository guidelines](AGENTS.md)
- [Issues](https://github.com/Apdelrahman1911/NativeCmpKit/issues)

## License and credits

NativeCmpKit is licensed under [Apache-2.0](LICENSE), with
[NOTICE](NOTICE) and [third-party notices](THIRD_PARTY_NOTICES.md).
Thanks to Mohamed Rejeb and the [original Calf contributors](https://github.com/MohamedRejeb/Calf)
for the code and iOS sample on which this toolkit builds. Original copyrights,
attribution and Git history are preserved. Additional credits include
[compose-cupertino](https://github.com/alexzhirkevich/compose-cupertino),
[AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass), Jodd and the
Cupertino icon font authors.
