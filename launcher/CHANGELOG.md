# Grid edition — Änderungsprotokoll / Change history

This log covers changes made by this fork, not personal TV configuration changes.
The original project is [humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv).
Local development iterations are grouped into their public release rather than
presented as separate releases.

## Documentation update — 2026-10-21

- Added a German feature overview and quick guide to the main README.
- Expanded picture-folder, language and remote-button setup instructions.
- Corrected outdated APK-size and permission claims; marked the original
  launcher's performance comparison as historical, not a Grid edition benchmark.
- Clarified Android 9+ compatibility, signature-compatible updates and the
  difference between `home.conf` and private last-selection state.
- Expanded this release history. No code, APK or on-device settings changed.
- Wallpapers are user-supplied local files, not bundled assets. TV-specific
  cleanup, settings and private files are outside this project's release history.

## 1.8 — 2026-10-21

App version `1.8-grid.10` · version code `12` · Android 9+ · APK 62,098 bytes.

[Release and APK](https://github.com/projectreturn/dumbtv/releases/tag/launcher-v1.8)
· [Changes from 1.2](https://github.com/projectreturn/dumbtv/compare/launcher-v1.2...launcher-v1.8)

### Oberfläche und Navigation / Interface and navigation

- Kept the clock and date visible in a fixed header while the app grid scrolls
  underneath a soft fade. Removed the redundant Apps heading.
- Added focus-driven scrolling with a 240 ms decelerating animation.
- Added a per-tile focus zoom and gently fading selection ring (160 ms).
  Replaced the experimental flying frame with this local tile effect.
- Continued left/right selection across row boundaries in both directions.
  The first and last apps stop at the ends of the full grid rather than looping.
- Kept the focused top row below the header when scrolling back upward.
- Replaced text header buttons with vector-drawn brush and settings icons;
  prevented focus highlights from being clipped at the header edges.
- Added four direction arrows in app move mode; inputs keep horizontal arrows.
- Restored the last focused app/input after returning from another screen or
  recreating the home activity. State is saved on pause, not on every key press.
  Missing/hidden apps fall back to the first tile; Back explicitly selects it.

### Fernbedienung / Remote control

- Added a configurable TV-button target under Appearance / Gestaltung.
  An unassigned TV button opens the app chooser instead.
- Mapped Settings/Notification to system settings and TV/Guide to the target app.
- Added the optional **dumbtv remote control / dumbtv Fernbedienung** accessibility
  key filter for shortcuts intercepted before the launcher, including use in
  other apps where the platform delivers those keys.
- Added an explanation and a link to Android Accessibility to enable/disable it.
  All unrelated key events pass through; manufacturer key handling may differ.

### Hintergrundbilder / Wallpapers

- Added a read-only, remote-friendly internal/USB folder browser, including on
  devices whose Android document picker is only a placeholder.
- Supported JPG/JPEG, PNG and WebP files directly in the selected folder;
  subfolders are not scanned. Folder browsing runs off the UI thread.
- Selecting a folder enables image backgrounds; dimming, next-image and existing
  rotation modes remain available (off, return home, hourly, daily, time of day).
- Added a Default folder action for the app-owned wallpapers directory and a
  plain-background fallback for unreadable, empty or unplugged folders.
- Kept local image files unchanged. No Google Ambient gallery integration,
  internet permission or online downloader was added.

### Sprache / Language

- Added offline German and English menus, dialogs and date formatting.
- Added **System language / Deutsch / English** in Appearance / Gestaltung.
  System mode uses German on German devices, English otherwise.
- Kept the choice local to the launcher; Android language and app names are not
  changed. Returning from the language chooser refreshes the launcher UI.

### Leistung und Wartung / Performance and maintenance

- Moved app discovery to a background executor and reused loaded artwork for
  unchanged packages; package changes invalidate only the affected artwork.
- Reduced repeated list lookups using sets/maps for visibility and ordering.
- Skipped input discovery when the inputs row is disabled and registered its
  callbacks only when needed.
- Kept animations event-driven with no idle redraw loop and no new libraries.
- Guarded asynchronous results against stale loads/destroyed activities and
  shut down background work when its owner is destroyed.
- Added language and permission regression tests alongside existing script tests.

### Überprüfung / Verification scope

- 27 automated tests passed for the release; catalog validation and release
  checksum checks passed. The release APK's manifest was checked as well.
- Tested on Android TV 14 with a 1920 × 1080 UI: navigation, retained selection,
  local picture-folder loading and switching between German and English.
- The owner confirmed physical Settings and TV remote buttons both worked, and
  that header and tiles were visible on the actual TV despite incomplete ADB
  screenshot captures. Such screenshots alone are not a rendering benchmark.
- No cross-device RAM or speed improvement is claimed for this release. The
  original README's Fire OS measurements do not measure the current Grid build.

### Permissions and privacy

There is no internet, storage-write or all-files-management permission. Local
wallpaper folders require read access approved by the owner. The optional remote
service receives key events to handle Settings/Notification and TV/Guide shortcuts;
it cannot retrieve screen content and can be disabled in Android Accessibility.
The original Fire OS 6 Keeper permissions remain limited to Android SDK 27 and below.

### Installation

The release APK requires Android 9 or newer. Install `dumbtv-home.apk` as an update
to the previous Grid edition signed by this fork, keeping the same package and
existing settings. Custom builds signed with a different key cannot update it.

Do not uninstall or reset the launcher just to update it. The source, build scripts
and checksums are included in the repository. The signing key, device settings,
screenshots and local development tools are not published.

## 1.2 — first public Grid edition

[Release](https://github.com/projectreturn/dumbtv/releases/tag/launcher-v1.2)

- Replaced the single horizontal app strip with a multi-row grid.
- Made the grid responsive to tile size: 6 columns for small, 5 for medium and
  4 for large, preserving app order and tile actions.
- Documented the fork and its grid behavior in the repository README.
- Retained the upstream launcher, HDMI inputs and TV-management tools.
