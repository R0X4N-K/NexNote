# Release Signature

Publish the SHA-256 fingerprint of the production signing certificate here after the first signed release.

Generate it with:

```bash
keytool -list -v -keystore release.keystore -alias YOUR_ALIAS
```

Do not commit the keystore or signing passwords.

Current status: no production signing fingerprint has been published yet.
