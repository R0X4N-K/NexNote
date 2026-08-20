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
512×512 RGBA sRGB PNG without a rounded outer mask or external shadow.

## Screenshots

The Fastlane screenshots show the current NexNote interface with only the
built-in Checklist template and the synthetic title “Weekly checklist”. They
contain no personal or third-party content. Both files are 1392×3120 RGBA sRGB
PNGs and are distributed with NexNote under `GPL-3.0-only`.

## Distributed assets

| Asset | SHA-256 | Provenance and license |
|---|---|---|
| `artwork/nexnote-icon-source.svg` | `36D9FE6AF65C137BBADE6C2748302C80AFE04CF30F1B6D73E2E207D998361194` | Original full-square source; GPL-3.0-only |
| `artwork/nexnote-logo.svg` | `999D40C69B506C98B9D213D7E8C2EB1A722047B359096F747D4F9A865E3F169B` | Original transparent source; GPL-3.0-only |
| `app/src/main/res/drawable/ic_launcher_background.xml` | `4B2EE5DA104FBB577C29D34799C79E0CD88B8016B9FB3BC288D120B02DC4CD06` | Derived background layer; GPL-3.0-only |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | `379498D1E51AE10272B94715E6E88E8A00EC819487AD4B464356CF0B03A31C5E` | Safe-zone transform of the original foreground; GPL-3.0-only |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | `EDE7653804F3B0801DB99581F34DF570635AC445682544D0D09E9A351B00CD71` | Single-color derivation of the original silhouette; GPL-3.0-only |
| both `app/src/main/res/mipmap-anydpi/ic_launcher*.xml` | `ADA31DA9E23F4520F3753E71CA23799A107431473DAA3774B6CECAC26847260C` | Adaptive-icon references to the three layers above |
| `fastlane/metadata/android/en-US/images/icon.png` | `675296FA07538046EF356039AFEFECE4AE59004892778CEDA96FA845D9FE68BA` | Production rendering of the same identity; GPL-3.0-only |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/01-home.png` | `8676CA3BC31C00186D10F29BDD657177CD6DEF6B9BA25F7D9E21C40F3518B56D` | Project screenshot with synthetic content; GPL-3.0-only |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/02-settings.png` | `7DB47D2E40BF18DDDABFAAE29640EECCD3FE98C53F86B87F35230FE7971AC60B` | Project screenshot without user content; GPL-3.0-only |

Source vectors, production exports, and the visual validation sheet are retained
under `artwork/`; `artwork/README.md` describes their relationships.

## Legacy assets

The density-specific Android Studio template launcher files were removed in
favor of the adaptive icon resources used by every supported platform
(`minSdk = 29`). They are not part of the current distribution.
