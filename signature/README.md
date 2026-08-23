# Release Signature

The upstream GitHub release APKs are signed with the NexNote production
certificate. The first signed release is
[`v1.0.0`](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.0).
Starting with `v1.0.1`, the release workflow uses Android Build Tools 34 and
omits legacy v1/JAR signature metadata so F-Droid can verify and publish the
upstream-signed APK as a reproducible build.

- Key alias: `nexnote-release`
- Certificate SHA-256 fingerprint:
  `74:C2:2D:14:EB:E9:4C:04:46:9A:6E:B5:46:EA:93:49:6E:C9:EB:72:3D:71:75:5B:7A:03:51:18:A6:48:78:A4`
- `NexNote-v1.0.0.apk` SHA-256:
  `24EDDE261B160640C1E6881FBB13C11281BE75E817CD7549D1E2F6C28C515F91`

Generate it with:

```bash
keytool -list -v -keystore release.keystore -alias YOUR_ALIAS
```

Do not commit the keystore or signing passwords. F-Droid metadata pins this
certificate with `AllowedAPKSigningKeys`, so GitHub and F-Droid releases share
the same Android signing identity after reproducible-build verification.
