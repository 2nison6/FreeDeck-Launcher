# FreeDeck Launcher APKs

Every released version of the app lives here, named `FreeDeck-Launcher-vX.Y.apk`, with its SHA-256 checksum in a
`.sha256` file next to it. Older versions stay so anyone can roll back.

| Version | File | Notes |
| --- | --- | --- |
| **1.0** | [`FreeDeck-Launcher-v1.0.apk`](FreeDeck-Launcher-v1.0.apk) | First release. Package `com.nison.freedeck`, 5.5 MB, signed with the FreeDeck release key. |

## Verify a download

```bash
shasum -a 256 -c FreeDeck-Launcher-v1.0.apk.sha256
```

## Install

See [Install](../README.md#install) in the main README.

Later versions must be signed with the same key as this one, so they install over it without losing settings.
