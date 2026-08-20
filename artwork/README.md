# NexNote visual identity sources

This directory preserves the source and production exports used by the Android
launcher and Fastlane/F-Droid metadata.

## Provenance and license

The NexNote logo is original project artwork. The project maintainer has
confirmed ownership of the design and licenses it with NexNote under
`GPL-3.0-only`. The canonical source files are:

- `nexnote-icon-source.svg` — SHA-256
  `36D9FE6AF65C137BBADE6C2748302C80AFE04CF30F1B6D73E2E207D998361194`;
- `nexnote-logo.svg` — SHA-256
  `999D40C69B506C98B9D213D7E8C2EB1A722047B359096F747D4F9A865E3F169B`.

No stock asset, font, linked resource, or external image is part of these SVGs.

## Production relationships

- `nexnote-icon-background.svg` is the full-bleed `#2D2D2A` background.
- `nexnote-icon-foreground.svg` preserves the supplied `#BBE1C3` paths and
  uniformly scales them to the Android adaptive-icon safe zone.
- `nexnote-icon-monochrome.svg` is the same geometry as a one-color silhouette.
- `nexnote-icon-combined.svg` combines the production background and scaled
  foreground without applying an outer mask.
- `nexnote-icon-master-2048.png` and `nexnote-icon-store-512.png` are sRGB RGBA
  exports of the combined production layout.
- `nexnote-icon-mask-preview.svg` and `.png` show circle, squircle,
  rounded-square, light themed, dark themed, and unmasked store renderings.

The measured foreground bounds are x=36.5–73.4 dp and y=22.5–86.1 dp within
the required x/y=21–87 dp safe zone. The Fastlane production icon is an exact
copy of `nexnote-icon-store-512.png`.

All files in this directory are distributed with NexNote under
`GPL-3.0-only`.
