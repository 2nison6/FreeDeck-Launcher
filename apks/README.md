# FreeDeck Launcher APKs

Each released version has its own folder, `vX.Y/`, containing only that version's files: the APK
(`FreeDeck-Launcher-vX.Y.apk`) and its SHA-256 checksum (`.sha256`). Older versions stay so anyone can roll back.

```
apks/
  v1.0/
    FreeDeck-Launcher-v1.0.apk
    FreeDeck-Launcher-v1.0.apk.sha256
  v1.1/   (next release)
```

| Version | File | Notes |
| --- | --- | --- |
| **1.0** | [`v1.0/FreeDeck-Launcher-v1.0.apk`](v1.0/FreeDeck-Launcher-v1.0.apk) | First release. Package `com.nison.freedeck`, 5.5 MB, signed with the FreeDeck release key. |

## Verify a download

```bash
cd v1.0
shasum -a 256 -c FreeDeck-Launcher-v1.0.apk.sha256
```

## Install

See [Install](../README.md#install) in the main README.

Later versions must be signed with the same key as this one, so they install over it without losing settings.
