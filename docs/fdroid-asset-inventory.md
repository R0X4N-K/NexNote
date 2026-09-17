# Asset provenance and licensing

This document records the source, derivation, and distribution status of the
visual assets shipped with NexNote.

## Canonical logo sources

The NexNote logo is original project artwork. The project maintainer has
confirmed ownership of the design and licenses it with NexNote under
`GPL-3.0-only`.

| Source asset | SHA-256 | Contents |
|---|---|---|
| `artwork/nexnote-icon-source.svg` | `36D9FE6AF65C137BBADE6C2748302C80AFE04CF30F1B6D73E2E207D998361194` | 2048×2048 full-square SVG with the production background and foreground paths |
| `artwork/nexnote-logo.svg` | `999D40C69B506C98B9D213D7E8C2EB1A722047B359096F747D4F9A865E3F169B` | 2048×2048 transparent SVG with the same foreground paths |

The canonical SVGs contain no font, embedded raster, filter, external link, or
external resource.

## Launcher and store derivations

The Android adaptive icon keeps the background and foreground in separate
layers. The original foreground geometry is scaled uniformly to 62.5% around
the canvas center. Its measured bounds are x=36.5–73.4 dp and y=22.5–86.1 dp,
inside Android's central x/y=21–87 dp safe zone.

The monochrome drawable uses the same silhouette as a single-color alpha shape.
Circle, squircle, rounded-square, themed-light, and themed-dark renderings are
shown in `artwork/nexnote-icon-mask-preview.png`.

The Fastlane icon is rendered from the same adaptive layout. It is a full-square
512×512 RGBA sRGB PNG without a rounded outer mask or external shadow. PNG text,
timestamp, and EXIF chunks are removed from every distributed image; color and
physical-dimension profiles are retained.

## Screenshots

The Fastlane screenshots predate the 1.0.4 restyling. They show the
release interface with only the
built-in Checklist template and the synthetic title “Weekly checklist”. They
contain no personal or third-party content. Both files are 1392×3120 RGBA sRGB
PNGs and are distributed with NexNote under `GPL-3.0-only`. Replace them with
screenshots of the release being prepared before publishing new store metadata.

## Distributed assets

| Asset | SHA-256 | Provenance and license |
|---|---|---|
| `artwork/nexnote-icon-source.svg` | `36D9FE6AF65C137BBADE6C2748302C80AFE04CF30F1B6D73E2E207D998361194` | Original full-square source; GPL-3.0-only |
| `artwork/nexnote-logo.svg` | `999D40C69B506C98B9D213D7E8C2EB1A722047B359096F747D4F9A865E3F169B` | Original transparent source; GPL-3.0-only |
| `app/src/main/res/drawable/ic_launcher_background.xml` | `4B2EE5DA104FBB577C29D34799C79E0CD88B8016B9FB3BC288D120B02DC4CD06` | Derived background layer; GPL-3.0-only |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | `379498D1E51AE10272B94715E6E88E8A00EC819487AD4B464356CF0B03A31C5E` | Safe-zone transform of the original foreground; GPL-3.0-only |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | `EDE7653804F3B0801DB99581F34DF570635AC445682544D0D09E9A351B00CD71` | Single-color derivation of the original silhouette; GPL-3.0-only |
| both `app/src/main/res/mipmap-anydpi/ic_launcher*.xml` | `ADA31DA9E23F4520F3753E71CA23799A107431473DAA3774B6CECAC26847260C` | Adaptive-icon references to the three layers above |
| `fastlane/metadata/android/en-US/images/icon.png` | `F49A3A87252F825CBEB4CA165903B6C349E4A2674EC11A53FAB9174015841E36` | Production rendering of the same identity; GPL-3.0-only |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/1.png` | `55A9433943FF4EA7075F6D51D171F1C1D20AB24067287140AD1D04685719DA7D` | Project screenshot with synthetic content; GPL-3.0-only |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/2.png` | `23C27498F193A1EAA9C842CA12B708200585872F055AA606586823D99BE421D7` | Project screenshot without user content; GPL-3.0-only |

Source vectors, production exports, and the visual validation sheet are retained
under `artwork/`; `artwork/README.md` describes their relationships.

## Bundled fonts

The Settings typeface preference uses unmodified variable fonts from the
`google/fonts` repository. They are licensed under the SIL Open Font License
1.1 (OFL-1.1). No reserved font name is
reused for a modified version, and the license text and copyright notices are
packaged in `THIRD_PARTY_NOTICES.md` under `assets/legal/`.

| Asset | SHA-256 | Provenance and license |
|---|---|---|
| `app/src/main/res/font/inter_variable.ttf` | `29160A80FF49DDCAB2C97711247E08B1FAB27A484A329CE8B813D820DC559031` | Inter, `ofl/inter/Inter[opsz,wght].ttf`; OFL-1.1, Copyright 2020 The Inter Project Authors |
| `app/src/main/res/font/lora_variable.ttf` | `822A6621CCBE8D97D20AC88C1C41F5615C9C2C202EAA75F272CD452AAC6475A7` | Lora, `ofl/lora/Lora[wght].ttf`; OFL-1.1, Copyright 2011 The Lora Project Authors (Reserved Font Name "Lora") |
| `app/src/main/res/font/fira_code_variable.ttf` | `9335B082B3C7850D98A64B584F3417F65355F3471278BB5EEB8C6C0E8657AEEB` | Fira Code, `ofl/firacode/FiraCode[wght].ttf`; OFL-1.1, Copyright 2014-2020 The Fira Code Project Authors |
| `app/src/main/res/font/jetbrains_mono_variable.ttf` | `48715A42EC242C21E9F02692891E147D022299A52E48D5E413E1A942193FFEDA` | JetBrains Mono, `ofl/jetbrainsmono/JetBrainsMono[wght].ttf`; OFL-1.1, Copyright 2020 The JetBrains Mono Project Authors |

The upstream license for each file is `OFL.txt` alongside the font in the
`google/fonts` repository, for example
<https://github.com/google/fonts/blob/main/ofl/firacode/OFL.txt>. The font
resources are loaded locally and require no network access.
