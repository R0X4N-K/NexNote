# Vault file streaming, version 2

New files start with the ASCII bytes
`nexnote-vault-file:2:tink-aes256-gcm-hkdf-64k:` followed by a Google Tink
AES-GCM-HKDF streaming ciphertext. The exact prefix is associated data, with
fixed HKDF-SHA256, a 32-byte derived AES key, 65,536-byte ciphertext segments
and first-segment offset zero. Input key material is the unlocked Vault key.
Tink generates a fresh salt and nonce prefix for each encryption. No custom
segment encryption or authentication algorithm is implemented by NexNote.

Tink authenticates segment order and the final segment. Every publishing
operation consumes the decrypting stream through EOF before syncing and
replacing its destination. A corrupt, truncated, appended or reordered payload
fails, leaving the original untouched. Temporary rekey backups contain only
ciphertext. The output stream is finalized before file-descriptor sync and rename.

Version 1 (`nexnote-vault-file:1:AES/GCM/NoPadding:...`) stays readable. New
writes use version 2; PIN rotation and duplicate re-encryption migrate touched
files. Unknown version prefixes are rejected before reading the whole file; the legacy
reader loads a version 1 payload before validating the rest of its envelope.
The legacy reader still allocates the historical payload, so constant-memory
claims apply to version 2. Downgrading to an app without version 2 support is
not supported after writing these files.

File operations use 8 KiB copy buffers and Tink's fixed 64 KiB segment buffers.
Document duplication uses a decrypting stream; only the image-decoding API
returns a complete byte array. PIN rotation streams between old and new keys
without a plaintext disk intermediate. Third-party/internal JCE buffers are not
claimed to be deterministically erased by the JVM.

The import journal is separate from Vault rekey rollback. Existing filesystem /
Room / DataStore crash-atomicity limitations during PIN changes remain; handled
exceptions roll back using ciphertext backups. The import journal does not provide
a transaction spanning all three stores. Similarly, restore-to-normal retains
the repository's existing database-before-file workflow.

References:

- [Tink streaming AEAD](https://developers.google.com/tink/streaming-aead)
- [AES-GCM-HKDF format](https://developers.google.com/tink/streaming-aead/aes_gcm_hkdf_streaming)
- [Tink Java source](https://github.com/tink-crypto/tink-java)

See [Running tests](testing.md) for streaming, corruption, and small-heap checks.
