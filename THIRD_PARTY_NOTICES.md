# NexNote third-party notices

This file lists third-party components and bundled font licenses. The exact
dependency graph is recorded in the Gradle lockfiles. NexNote itself is distributed under
GPL-3.0-only; the complete project license is packaged next to this file in the
APK and remains available as the repository-root `LICENSE` file.

This file does not assign copyright in NexNote or in its graphic assets. The
maintainer has confirmed ownership of the original artwork and licenses it under
`GPL-3.0-only`; the provenance record is kept in `artwork/README.md` and
`docs/fdroid-asset-inventory.md`. Third-party component copyrights remain with
their respective holders.

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

## License packaging

The build packages this document and the complete GPL-3.0-only project license
under `assets/legal/`. Check their presence when reviewing a release APK.

The Apache License 2.0 text is retained by AndroidX entries under `META-INF/`
and its upstream text is available at
<https://www.apache.org/licenses/LICENSE-2.0>.

## MaterialKolor color utilities

NexNote uses `com.materialkolor:material-color-utilities:3.0.0` (Android
variants) for HCT, tonal schemes and hue harmonization. This Kotlin port is
maintained by Jordon de Hoog: https://github.com/jordond/MaterialKolor.
Its Google Material Color Utilities algorithms retain their Apache-2.0
copyright headers (Copyright 2021-2025 Google LLC); the Apache license is
included under the APK's `META-INF/` entries described above. The Kotlin port
is distributed under the following MIT license.
The runtime dependency `dev.drewhamilton.poko:poko-annotations:0.19.0`
is Apache-2.0: https://github.com/drewhamilton/Poko.

MIT License

Copyright (c) 2025 Jordon de Hoog

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Vault streaming cryptography

Vault file encryption uses `com.google.crypto.tink:tink:1.18.0` (Apache-2.0),
with Gson 2.10.1 and Error Prone annotations 2.22.0 (Apache-2.0), and
`com.google.protobuf:protobuf-java:4.28.2` (BSD-3-Clause).
Tink source and license: https://github.com/tink-crypto/tink-java/tree/v1.18.0

Protocol Buffers license (https://github.com/protocolbuffers/protobuf/blob/v28.2/LICENSE):

```text
Copyright 2008 Google Inc.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Code generated by the Protocol Buffer compiler is owned by the owner
of the input file used when generating it.  This code is not
standalone and requires a support library to be linked with it.  This
support library is itself covered by the above license.

```

## Bundled fonts

The app offers a typeface preference in Settings. The
non-system options are bundled as variable font resources under
`app/src/main/res/font/`, taken unmodified from the `google/fonts`
repository. All four families are licensed under the SIL Open Font
License 1.1 (OFL-1.1), a free and open license compatible with
GPL-3.0-only distribution. No reserved font name is used by NexNote for a
modified version; the files are redistributed verbatim.

| Resource | Family | Copyright holder | Upstream source |
|---|---|---|---|
| `res/font/inter_variable.ttf` | Inter | The Inter Project Authors | <https://github.com/rsms/inter> |
| `res/font/lora_variable.ttf` | Lora | The Lora Project Authors, Reserved Font Name "Lora" | <https://github.com/cyrealtype/Lora-Cyrillic> |
| `res/font/fira_code_variable.ttf` | Fira Code | The Fira Code Project Authors | <https://github.com/tonsky/FiraCode> |
| `res/font/jetbrains_mono_variable.ttf` | JetBrains Mono | The JetBrains Mono Project Authors | <https://github.com/JetBrains/JetBrainsMono> |

The exact files were fetched from
<https://github.com/google/fonts> (`ofl/inter`, `ofl/lora`,
`ofl/firacode`, `ofl/jetbrainsmono`) and are recorded with their SHA-256
digests in `docs/fdroid-asset-inventory.md`.

SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded,
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the
copyright statement(s).

"Original Version" refers to the collection of Font Software components as
distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting,
or substituting -- in part or in whole -- any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.

"Author" refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the corresponding
Copyright Holder. This restriction only applies to the primary font name as
presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to promote, endorse or advertise any
Modified Version, except to acknowledge the contribution(s) of the
Copyright Holder(s) and the Author(s) or with their explicit written
permission.

5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are
not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL
DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM
OTHER DEALINGS IN THE FONT SOFTWARE.
