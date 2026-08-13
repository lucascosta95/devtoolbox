<div align="center">

<img src="docs/icon.png" width="120" alt="DevToolbox">

# DevToolbox

**23 developer tools in one desktop app. Offline, instant, no telemetry.**

[![build](https://github.com/lucascosta95/devtoolbox/actions/workflows/build.yml/badge.svg)](https://github.com/lucascosta95/devtoolbox/actions/workflows/build.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4)
![Platforms](https://img.shields.io/badge/Linux%20·%20macOS%20·%20Windows-3f424d)

**English** · [Português](README.pt-BR.md)

<img src="docs/screenshot-dark.png" width="820" alt="DevToolbox in dark theme">

</div>

---

## Why it exists

You open one random website to decode a JWT, another to format JSON, a third to validate a
document number — and paste work data into servers you know nothing about.

DevToolbox does all of that **on your machine**. No tool touches the network, nothing is sent
anywhere, and the input you type is never written to disk.

## The tools

| Category | Tools | |
|---|---|---|
| **Encoding** | Base64 · JWT Decoder · URL Encode/Decode · Hash (MD5/SHA-1/SHA-256) · Image → Base64 | 5 |
| **Formatters** | JSON · YAML · cURL · SQL · NRQL · Text and JSON diff | 6 |
| **Text** | String Case (8 formats) · Regex Tester · Lorem Ipsum | 3 |
| **Generators** | UUID v4 · Colors (HEX/RGB/HSL/OKLCH) · Timestamp · Cron · QR Code | 5 |
| **Validators** | CPF · CNPJ · Brazilian phone number · Credit card (Luhn) | 4 |

Everything processes **real input**, recalculating as you type, with errors that explain what is
wrong — not just "invalid":

> `Invalid JSON at line 3, column 12: expected ':' after key "b"`

<div align="center">
<img src="docs/screenshot-diff.png" width="410" alt="Diff">
<img src="docs/screenshot-qr.png" width="410" alt="QR Code">
</div>

## Installation

Download the installer for your platform from **[Releases](https://github.com/lucascosta95/devtoolbox/releases)**:

| Platform | File | Note |
|---|---|---|
| Linux | `.deb` | `sudo apt install ./devtoolbox_*.deb` |
| Windows | `.msi` | SmartScreen warns on first run (unsigned) |
| macOS | `.dmg` | Settings → Privacy & Security → "Open Anyway" |

All of them bundle the JVM — **no Java installation required**.

## Shortcuts

| | |
|---|---|
| `Ctrl/Cmd` + `K` or `F` | Focus the search |
| `↑` `↓` | Navigate the filtered list |
| `Esc` | Clear the search |
| `Ctrl/Cmd` + `D` | Favorite the active tool |
| `Ctrl/Cmd` + `Shift` + `L` | Toggle light/dark |

Favorites, theme, accent color and the last open tool are remembered across runs
(`~/.config/devtoolbox` on Linux, `Application Support` on macOS, `%APPDATA%` on Windows).

<div align="center">
<img src="docs/screenshot-light.png" width="820" alt="DevToolbox in light theme">
</div>

## Architecture

Three modules, with one rule holding up the rest: **`:core-tools` knows nothing about the UI**.

```
:core-tools     pure logic in commonMain — no Compose, no I/O, no network
    ↑
:designsystem   Nocturne tokens, components and the 9 layout archetypes
    ↑
:app-desktop    window, packaging, icons
```

Every tool implements a single-function contract:

```kotlin
interface Tool {
    val id: String
    val name: String
    val category: Category
    val defaultInput: ToolInput

    fun run(input: ToolInput): ToolOutput   // pure and synchronous
}
```

Because `run` is pure, every tool is testable without a UI, without a clock and without mocks.
And because the output is a **layout descriptor** (`ToolBody.Io`, `.Rows`, `.Diff`, `.Validate`…),
the interface already knows how to draw any new tool without a line of UI code.

**Adding a tool:** one class, one line in `ToolRegistry`, one test.

### Zero dependencies in the core

`:core-tools` has no runtime dependencies. MD5, SHA-1, SHA-256, Base64, percent-encoding, JSON,
a YAML subset, cron, OKLCH conversion, the Luhn algorithm, image header parsing and the QR Code
encoder are implemented in the project — in `commonMain`, ready for Android, iOS or Wasm without
changing a line.

## Development

```bash
./gradlew :app-desktop:run          # run the app
./gradlew test                      # 281 tests
./gradlew :app-desktop:screenshot   # render every screen to PNG, no display needed
./gradlew :app-desktop:packageDeb   # installer for the current platform
```

Requires **JDK 21**. Gradle comes from the wrapper.

`screenshot` renders the UI headlessly via `ImageComposeScene` — one image per tool, plus the
search and error states. That is what makes it possible to review visual changes without opening
the window.

The icons (`.png`, `.ico`, `.icns`) are generated from the design system tokens by the `appIcon`
task, which packaging depends on — that is why they are not versioned.

### Tests

Every transformation and every validator has a test, always including invalid input:

- hashes checked against the official RFC 1321 and FIPS 180 vectors
- QR Code cross-checked with **ZXing** (test classpath only — the core stays dependency-free)
- color round-trip HEX ↔ RGB ↔ HSL ↔ OKLCH
- CPF/CNPJ with check digits recomputed by an independent implementation in the test
- credit card with the public test numbers from the card networks (Luhn)
- image with encode/decode round-trip and format detection by magic bytes

## CI

`.github/workflows/build.yml` runs on push to `main`, on pull requests and on demand:

1. **tests** on Ubuntu, publishing the reports as an artifact when they fail
2. **installers** in an `ubuntu` / `windows` / `macos` matrix, producing `.deb`, `.msi` and `.dmg`
3. **release** only on `v*` tags, bundling the three installers

```bash
git tag v1.3.0 && git push origin v1.3.0
```

## Known limitations

- **JWT** decodes header and payload, but **does not verify the signature**
- **YAML** covers a subset: block and flow maps and sequences, simple scalars and comments.
  Anchors, multiple documents, tags and block scalars are rejected with an explicit error
- **Cron** accepts 5 fields with `*`, number, range, step and list — no `L`, `W`, `#` or `@daily`
- **QR Code** in byte mode, error correction level M, versions 1–10 (up to 213 bytes)
- **Image → Base64** accepts up to 5 MB, in PNG, JPG, GIF, WebP, BMP, ICO and SVG
- The installers are **not signed**

## Credits

[Phosphor](https://phosphoricons.com) icons · [JetBrains Mono](https://www.jetbrains.com/lp/mono/)
typeface (OFL) · Nocturne design system.

## License

MIT
