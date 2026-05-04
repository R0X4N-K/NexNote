# GitHub Publish Notes

The intended private repository is:

```text
https://github.com/R0X4N-K/NexNote
```

Repository settings:

- Visibility: private for now.
- Default branch: `main`.
- Description: Local-first Android note-taking app built with Kotlin and Jetpack Compose.
- Suggested topics for the future public repository: `android`, `kotlin`, `jetpack-compose`, `note-taking`, `fdroid`, `material3`.

## Publish Commands

Use these commands from the project root after creating the empty private repository on GitHub:

```powershell
git init -b main
git add .
git commit -m "Initial repository setup"
git remote add origin https://github.com/R0X4N-K/NexNote.git
git push -u origin main
```

If a broken local `.git` directory already exists because a sandboxed tool initialized it under a different Windows user, remove only that `.git` directory first, then run the commands above again.

```powershell
Remove-Item -Recurse -Force .git
```

Do not remove any application source, Gradle, Fastlane, or documentation files.

## After First Push

- Confirm the `Build` workflow runs green.
- Add the release signing secrets before pushing a `v*` tag.
- Keep the repository private until the first public release plan is ready.
- Before making it public, run a secret scan and confirm `local.properties`, `.idea/`, `.gradle/`, `app/build/`, keystores, APKs, and logs are not tracked.
