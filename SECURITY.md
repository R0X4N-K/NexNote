# Security Policy

## Supported Versions

NexNote is in early development. Security fixes will target the latest code on `main` until public releases begin.

## Reporting A Vulnerability

For now, report vulnerabilities privately to the repository owner. Once the repository becomes public, prefer GitHub private vulnerability reporting if it is enabled.

Please include:

- Affected version or commit.
- Clear reproduction steps.
- Impact and any relevant logs.
- Whether the issue involves local data, exported files, backups, or Android intents.

Do not publish working exploits or sensitive user data in public issues.

## Signing Key

No production signing fingerprint is published yet. After the first signed release, add the SHA-256 certificate fingerprint to `signature/README.md` and keep using the same release key for all future updates.

Never commit keystores, passwords, tokens, or signing property files.
