# Architecture and maintainer map

## Scope and how to use this knowledge base

NativeCmpKit uses `kira-*` artifact IDs and
`io.github.apdelrahman1911.kira` Kotlin packages. See the [installation and migration
guide](../installation.md) for the current coordinates and import changes.
The detailed audit and its counts describe upstream commit
`af388bd3ad0325b2afdd8c0149cb7ff6f24c5da6`. Historical paths and line numbers refer
to that revision and can be opened with `git show af388bd:<path>`.

Read this overview, then [execution flows](execution-flows.md),
[dependencies](dependency-map.md), [source inventory](source-inventory.md), and the
[platform matrix](../platforms/README.md). The detailed module reports contain
function-by-function analysis, defects, tests and limitations:

| Responsibility | Detailed report |
|---|---|
| Adaptive controls, native managers, geometry, state, animation | [UI](../modules/ui.md) |
| Files, picker/saver, Rust dialogs, MIME mapping, Coil | [I/O and file picker](../modules/io-and-file-picker.md) |
| Permission registry, Android/iOS helpers, location prototype | [Permissions and geo](../modules/permissions-and-geo.md) |
| Camera, share, WebView, cookie stores, excluded prototypes | [Native features](../modules/native-features.md) |
| Core contexts, navigation, symbols/font, sample app | [Navigation/core/icons/sample](../modules/navigation-core-icons-sample.md) |

The baseline inventory covers **607 tracked files**: 437 Kotlin, 38 Kotlin scripts, two
Rust and two Swift files, plus resources/configuration/documentation. Kotlin,
Kotlin scripts and Rust total **43,653 physical lines**; including Swift gives
43,708. Three explicitly generated Kotlin sources contribute 9,693 lines.
There are 15 test-source files, of which 13 contain 86 `@Test` annotations. These
are inventory counts, **not passing-test or semantic-review counts**.

## Product structure

NativeCmpKit is a collection of independently consumable KMP libraries.
Most public APIs are Compose functions which
remember native adapters or mutable observable state. It does not own an
application-wide service container, navigation lifecycle, filesystem, permission
policy, or UI thread dispatcher. Host applications supply those environments.

```mermaid
flowchart TD
    APP[Application / sample shells] --> UI[kira-ui]
    APP --> PICK[kira-file-picker]
    APP --> CAM[kira-camera-picker]
    APP --> SHARE[kira-share]
    APP --> WEB[kira-webview]
    APP --> PERM[kira-permissions aggregate or feature modules]
    APP --> NAV[kira-navigation]
    APP --> GEO[kira-geo]
    APP --> ICON[kira-cupertino-icons]
    APP --> COIL[kira-file-picker-coil]
    UI --> CORE[kira-core]
    PICK --> IO[kira-io]
    CAM --> IO
    SHARE --> IO
    COIL --> IO
    COIL --> CORE
    IO --> CORE
    PERM --> PC[kira-permissions-core]
    IO --> KIO[kotlinx-io RawSource]
    PICK --> JNI[Desktop JNI / Rust rfd]
    SHARE --> NATIVE[Desktop JNI / Rust AppKit]
    WEB --> FX[Desktop JavaFX]
```

Arrows show dependencies. The [current Maven inventory](../publishing/coordinates.json)
lists the Kira dependencies for each artifact. WebView, permissions, navigation
and geo consume Compose and platform APIs directly.

### Included libraries versus publishable libraries

`settings.gradle.kts` keeps the permission feature directories nested. The build
publishes each feature under its own flat artifact ID (for example,
`kira-permissions-core`).

| Module / family | Role and principal abstraction | Maven candidate |
|---|---|---|
| `kira-core` | `PlatformContext`, composition local, experimental/internal opt-ins | Yes |
| `kira-io` | `KmpFile`, metadata/read helpers, `RawSource`, context-free overloads | Yes |
| `kira-file-picker` | File type/selection/settings models, remembered picker/saver, native bridge | Yes |
| `kira-file-picker-coil` | `KmpFileFetcher.Factory` and Coil context adapter | Yes |
| `kira-camera-picker` | Remembered capture launcher returning `KmpFile` | Yes |
| `kira-share` | `ShareContent`, `ShareResult`, remembered launcher | Yes |
| `kira-webview` | Content/state/navigation/settings/cookie API and embedded platform web engines | Yes |
| `kira-ui` | Adaptive controls with Material/UIKit/Compose renderers | Yes |
| `kira-cupertino-icons` | Font codepoints and Compose icon rendering | Yes |
| `kira-permissions` | Optional bundle re-exporting all permission subprojects | Yes |
| `kira-permissions-core` | Permission identity/status/state, registries, storage/SMS/call mappings | Yes |
| Ten permission features | Camera, gallery, location, background-location, Bluetooth, contacts, calendar, notifications, Wi-Fi, microphone | Yes, each |
| `kira-navigation` | Experimental in-memory adaptive route stack and controller API | Yes |
| `kira-geo` | Experimental location models and limited tracker implementations | Yes |
| `kira-maps` | Build file only | No |

The 23 Maven candidates include the notification permissions leaf. `kira-media`
and the unintegrated `kira-notifications` iOS scheduling experiment sit outside
the active build.

## Source-set architecture

`convention-plugins/src/main/kotlin/kotlin.multiplatform.gradle.kts` applies KMP
and the AGP KMP Android library plugin, then configures hierarchy/targets.
`compose.multiplatform.gradle.kts` adds Compose and its compiler. The shared
target list is Android, JVM named `desktop`, browser JS, browser WasmJS,
`iosArm64`, and `iosSimulatorArm64`.

| Source set with handwritten code | Targets consuming that code | Reason |
|---|---|---|
| `commonMain` | All declared library targets | Public contracts/models and shared algorithms |
| `materialMain` | Android, desktop, JS, WasmJS | Material3 actuals for adaptive controls |
| `nonAndroidMain` | Desktop, iOS, JS, WasmJS | Context sentinel, context adapters and shared non-Android helpers |
| `webMain` | JS and WasmJS | Browser File/DOM/iframe/share APIs; language-specific bridges remain below |
| `iosMain` | Arm64 device and Arm64 simulator | UIKit, Photos, Foundation, CoreLocation and other Objective-C APIs |
| `androidMain` | Android | Context/URI, ActivityResult, native views and permissions |
| `desktopMain` | Desktop JVM | Swing/AWT/JavaFX, local files, JNI bridges |
| `jsMain`, `wasmJsMain` | Corresponding browser target | Different JS interop representations, Promise/typed-array/eval adapters |

`Hirearchy.kt` deliberately creates overlapping groups. In particular Android's
new AGP target must be matched by `KotlinMultiplatformAndroidLibraryTarget`, not
KGP's old `KotlinAndroidTarget`, or Material actuals disappear from Android.
Do not flatten these groups while renaming packages. Default Kotlin hierarchy
groups may also exist without any tracked sources; the table is not a dump of
Gradle's evaluated dependency graph.

`iosX64` was deliberately removed in upstream PR #515. There is no active
Linux/Windows/macOS Kotlin/Native target and no Node.js target; “Desktop” means
JVM plus host-specific libraries. A file ending `.desktop.kt` is not evidence
it works on every desktop architecture. See the platform matrix for no-ops and
host restrictions.

## Boundary design and consequences

1. **Compose owns remembered objects; the OS owns presentation.** A launcher or
   manager typically lives in `remember`; `LaunchedEffect` pushes state into it;
   `DisposableEffect` is supposed to release views/delegates/observers. The native
   lifetime can outlast composition or a coroutine. Review disposal and late
   callbacks together, not as isolated functions.
2. **Common API shape is not common behavior.** Many UI functions accept Material
   content slots plus separate `ios*` descriptors. UIKit does not render arbitrary
   Compose slots inside a native alert/navigation bar. Some unsupported APIs
   compile and silently do nothing (camera on desktop/web, permissions there,
   geo tracking away from Android). Keep these limits explicit.
3. **Material3 types are part of public contracts.** Colors, `SelectableDates`,
   picker states and scroll behaviors bind the ABI to the selected Compose/
   Material versions. This is not an implementation-only dependency that can be
   updated without downstream compatibility checks.
4. **`PlatformContext` is intentionally thin.** Android actualizes it to
   `android.content.Context`; other targets use a singleton sentinel. It is not
   a storage abstraction or a UIKit controller. IO's `AndroidContextHolder`
   enables no-context file helpers only after initialization; the baseline file
   picker composable initializes it. There is no manifest provider initializer.
   Standalone IO/camera/share consumers should use explicit context overloads.
5. **`KmpFile` is an opaque platform handle, not a portable path.** It wraps Android
   `Uri`, JVM `File`, iOS readable/original `NSURL`, or browser `File`. Baseline
   `getPath()` may be a URI, and an iOS original URL may lose access. Opening
   `source()` is the intended stream boundary; browsers still buffer first.
6. **Native bridges have two compatibility surfaces.** Kotlin package/class names
   and Rust `Java_io_github_apdelrahman1911_kira_...` exports must agree. Packaged native
   resources, when present, are selected by library name and OS/architecture
   directory; no `.so`, `.dll` or `.dylib` is tracked in the audited source tree.
   Changing only Maven group is not the same as changing these ABI names.
7. **Permissions are identities with lazy registration.** Accessing each
   `Permission.Companion.*` property initializes an object which registers an
   Android string mapping or iOS delegate factory. They are not an enum switch
   anymore. Including the umbrella pulls all feature references into the app;
   prefer narrow features when iOS privacy purpose strings matter.
8. **Navigation has no native screen lifecycle.** The implemented common host renders
   one route inside Compose. UIKit bars are overlaid onto a hosting controller,
   not managed by a UINavigationController per destination. Native iOS gesture
   transitions cannot be inferred from the `AdaptiveNavHost` name.

## Generated, vendored and handwritten boundaries

| Boundary | Evidence / maintenance rule |
|---|---|
| `calf-ui/.../sf/symbols/SFSymbol.kt` | Generated-marker file: 6,984 SF Symbol **name strings**, 7,006 lines; actual images are requested from UIKit. No generator is tracked. Do not mistake strings for redistributed Apple icon artwork. |
| `calf-cupertino-icons/.../CupertinoIcons.kt` | Generated-marker codepoint table: 1,322 constants, 1,335 lines; paired with the bundled font. |
| `sample/common/.../CupertinoIconsCatalogData.kt` | Generated-marker sample catalogue: 1,352 lines; not production behavior. |
| `calf-cupertino-icons/src/commonMain/composeResources/font/sf_symbols.ttf` | Binary font, misleading filename; provenance/license analysis is in the icons audit. It is separate from UIKit SF Symbol availability. |
| `calf-file-picker/.../jodd/net/MimeTypes.kt` and resources | Vendored MIME database/parser; not generated native bindings. Preserve attribution and review the parser as executable code. |
| `calf-file-picker/native/src/lib.rs`, `calf-share/native/src/lib.rs` | Handwritten unsafe JNI/native bridges, built by Cargo. No generated JNI wrapper validates the ABI for you. |
| `calf-webview/src/iosMain/cinterop/nskeyvalueobserving.def` | Handwritten 15-line Objective-C protocol embedded in a Kotlin/Native cinterop definition; compiler-generated stubs are build outputs. |
| `convention-plugins`, version catalog, Cargo manifests, workflows | Handwritten build logic/configuration. Gradle-generated resources/accessors, Kotlin native caches and downloaded binaries are not baseline source files. |

The removed upstream `external/backdrop` and `external/shapes` trees remain in
Git history but are not active source. Current iOS UI consumes published Kyant
libraries. This preserves the source lineage, not an offline copy of every
third-party dependency ever used.

## Application shells and extension points

`sample:common` composes the catalogue/application, uses local project
dependencies and configures the `Common` static iOS framework. Its framework
explicitly exports `calf-ui`; removing that export can affect Objective-C
target/action visibility. Android's `MainActivity`, desktop's AWT application,
browser `ComposeViewport` entrypoints, and `sample/ios/Calf.xcodeproj` host it.
The Xcode project is not a Gradle subproject. Demo screens exercise features but
are not automated regression tests or proof of native device compatibility.

For a new feature, first decide which owner manages callbacks/resources, which
targets genuinely implement it, and how “unavailable”, cancellation and errors
are reported. Add the public common contract, shared logic, platform actuals,
tests, capability documentation and consumer setup together. Keep no-op
implementations visibly unsupported until a compatible error/capability API is
designed. Do not add a new umbrella dependency merely to make a sample compile.

## Audit navigation

- [Audit overview](../audit/README.md), [historical review](../audit/historical-review.md)
- [Preserved upstream design history](../decisions/0002-upstream-design-history.md)
- [Testing evidence and unavailable environments](../testing/README.md)
- [Prioritized roadmap](../roadmap/README.md)

The audit is a baseline understanding, not a declaration of release readiness.
Confirmed defects, source-addressed historical reports and unverified platform
risks are deliberately kept separate.
