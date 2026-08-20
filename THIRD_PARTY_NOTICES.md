# NexNote third-party notices

This inventory was verified on 2026-08-16 against the resolved
`releaseRuntimeClasspath`. The exact dependency graph is also recorded by the
Gradle dependency lock state. NexNote itself is distributed under
GPL-3.0-only; the complete project license is packaged next to this file in the
APK and remains available as the repository-root `LICENSE` file.

This file does not assign copyright in NexNote or in its graphic assets. Those
facts are tracked separately and are recorded only after confirmation by the
relevant rights holder.

## Runtime components

All runtime artifacts in the following inventory are licensed under the Apache
License 2.0. Module names are the concrete artifacts selected by Gradle, not
merely the requested catalog aliases.

| Component family | Resolved version(s) | Resolved modules |
|---|---|---|
| AndroidX Activity | 1.8.2 | `activity`, `activity-compose`, `activity-ktx` |
| AndroidX Annotation | 1.4.1, 1.9.1 | `annotation-experimental`, `annotation-jvm` |
| AndroidX Arch Core | 2.2.0 | `core-common`, `core-runtime` |
| AndroidX Autofill | 1.0.0 | `autofill` |
| AndroidX Collection | 1.5.0 | `collection-jvm`, `collection-ktx` |
| AndroidX Compose Animation | 1.10.4 | `animation-android`, `animation-core-android` |
| AndroidX Compose Foundation | 1.10.4 | `foundation-android`, `foundation-layout-android` |
| AndroidX Compose Material | 1.7.8, 1.10.4 | `material-icons-core-android`, `material-icons-extended-android`, `material-ripple-android` |
| AndroidX Compose Material 3 | 1.4.0 | `material3-android` |
| AndroidX Compose Runtime | 1.10.4 | `runtime-android`, `runtime-annotation-android`, `runtime-retain-android`, `runtime-saveable-android` |
| AndroidX Compose UI | 1.10.4 | `ui-android`, `ui-geometry-android`, `ui-graphics-android`, `ui-text-android`, `ui-tooling-preview-android`, `ui-unit-android`, `ui-util-android` |
| AndroidX Concurrent | 1.1.0 | `concurrent-futures` |
| AndroidX Core | 1.0.0, 1.16.0 | `core-viewtree`, `core`, `core-ktx` |
| AndroidX Custom View | 1.0.0 | `customview-poolingcontainer` |
| AndroidX DataStore | 1.1.4 | `datastore-android`, `datastore-core-android`, `datastore-core-okio-jvm`, `datastore-preferences-android`, `datastore-preferences-core-jvm`, `datastore-preferences-external-protobuf`, `datastore-preferences-proto` |
| AndroidX DocumentFile | 1.0.0 | `documentfile` |
| AndroidX Dynamic Animation | 1.0.0 | `dynamicanimation` |
| AndroidX Emoji2 | 1.4.0 | `emoji2` |
| AndroidX ExifInterface | 1.3.7 | `exifinterface` |
| AndroidX Graphics | 1.0.1 | `graphics-path` |
| AndroidX Interpolator | 1.0.0 | `interpolator` |
| AndroidX Legacy | 1.0.0 | `legacy-support-core-utils` |
| AndroidX Lifecycle | 2.9.4 | common, LiveData, process, runtime and ViewModel artifacts selected for Android/JVM |
| AndroidX Loader | 1.0.0 | `loader` |
| AndroidX LocalBroadcastManager | 1.0.0 | `localbroadcastmanager` |
| AndroidX Navigation | 2.9.0 | `navigation-common-android`, `navigation-compose-android`, `navigation-runtime-android` |
| AndroidX Print | 1.0.0 | `print` |
| AndroidX Profile Installer | 1.4.0 | `profileinstaller` |
| AndroidX Room | 2.7.0 | `room-common-jvm`, `room-ktx`, `room-runtime-android` |
| AndroidX Saved State | 1.3.2 | `savedstate-android`, `savedstate-compose-android`, `savedstate-ktx` |
| AndroidX SQLite | 2.5.0 | `sqlite-android`, `sqlite-framework-android` |
| AndroidX Startup | 1.1.1 | `startup-runtime` |
| AndroidX Tracing | 1.2.0 | `tracing` |
| AndroidX Transition | 1.6.0 | `transition` |
| AndroidX VersionedParcelable | 1.1.1 | `versionedparcelable` |
| AndroidX Window | 1.5.0 | `window`, `window-core-android` |
| Guava ListenableFuture | 1.0 | `com.google.guava:listenablefuture` |
| Okio | 3.4.0 | `com.squareup.okio:okio-jvm` |
| Kotlin standard library | 1.8.0, 1.9.22, 2.2.10 | stdlib, JDK compatibility, parcelize and Android extensions runtime artifacts |
| kotlinx.coroutines | 1.10.1 | `kotlinx-coroutines-android`, `kotlinx-coroutines-core-jvm` |
| kotlinx.serialization | 1.7.3 | `kotlinx-serialization-core-jvm` |
| JetBrains annotations | 23.0.0 | `org.jetbrains:annotations` |
| JSpecify | 1.0.0 | `org.jspecify:jspecify` |

Upstream license sources:

- AndroidX and Compose: <https://android.googlesource.com/platform/frameworks/support>
- Kotlin: <https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt>
- kotlinx.coroutines: <https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt>
- kotlinx.serialization: <https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt>
- Okio: <https://github.com/square/okio/blob/3.4.0/LICENSE.txt>
- Guava/ListenableFuture: <https://github.com/google/guava>
- JetBrains annotations: <https://github.com/JetBrains/java-annotations>
- JSpecify: <https://github.com/jspecify/jspecify>

The release APK contains native libraries supplied by AndroidX:

- `libandroidx.graphics.path.so`, from `androidx.graphics:graphics-path:1.0.1`;
- `libdatastore_shared_counter.so`, from AndroidX DataStore 1.1.4.

Both are present for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` and are
covered by the AndroidX Apache-2.0 license above.

## Test-only components

These components are used to build or run tests and are not runtime
dependencies of the release APK.

| Component | Version | License |
|---|---:|---|
| JUnit 4 | 4.13.2 | EPL-1.0 |
| AndroidX Test JUnit | 1.1.5 | Apache-2.0 |
| AndroidX Espresso Core | 3.5.1 | Apache-2.0 |
| AndroidX Room Testing | 2.7.0 | Apache-2.0 |
| AndroidX Compose UI test libraries | 1.10.4 via BOM | Apache-2.0 |
| kotlinx.coroutines test | 1.10.1 | Apache-2.0 |

JUnit's EPL-1.0 license therefore does not describe code shipped in the
release APK and is not a runtime-license incompatibility.

## Build-only components

These tools are required to produce or verify the application, but are not
linked into the release runtime:

| Component | Version | License |
|---|---:|---|
| Gradle | 9.3.1 | Apache-2.0 |
| Android Gradle Plugin | 9.1.1 | Apache-2.0 |
| Kotlin Compose Gradle plugin | 2.2.10 | Apache-2.0 |
| Kotlin Symbol Processing | 2.3.9 | Apache-2.0 |
| AndroidX Room compiler | 2.7.0 | Apache-2.0 |

Build-tool license sources:

- Gradle: <https://github.com/gradle/gradle>
- Android Gradle Plugin: <https://android.googlesource.com/platform/tools/base>
- KSP: <https://github.com/google/ksp>

## NOTICE and packaging audit

The 95 unique resolved runtime archives were inspected for top-level
`LICENSE`, `NOTICE`, and `COPYING` entries. Fifty-three Apache-2.0 `LICENSE.txt`
entries were found and no upstream `NOTICE` entry was present. Before this
inventory was packaged, only six of those identical AndroidX license entries
survived Android resource packaging. The build now always adds this inventory
and the complete GPL-3.0-only project license under `assets/legal/`; final APK
inspection verifies their presence.

The Apache License 2.0 text is retained by AndroidX entries under `META-INF/`
and its upstream text is available at
<https://www.apache.org/licenses/LICENSE-2.0>.
