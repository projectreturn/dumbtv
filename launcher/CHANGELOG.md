# dumbtv home 1.8 — Grid edition

Changes since the first public Grid edition (1.2):

- Fixed clock/date header with a soft fade into the scrolling app grid.
- Smooth scrolling and a gently fading selection border, without a flying frame.
- Left/right navigation continues across row boundaries in both directions.
- Last selected app/input is restored on return and when the home activity restarts.
- German, English and system-language selection under Appearance / Gestaltung.
- Brush and settings icons with unclipped focus highlights.
- Four-direction arrows when rearranging apps.
- Configurable TV-button app and an optional remote-control accessibility service.
- Read-only folder picker for local and attached USB wallpaper folders, supporting
  JPG/JPEG, PNG and WebP files directly in the selected folder.
- Background app discovery, targeted artwork refreshes and fewer unnecessary
  catalog/input operations.
- Automated language and permission checks alongside the existing script tests.

## Permissions and privacy

There is no internet, storage-write or all-files-management permission. Local
wallpaper folders require read access approved by the owner. The optional remote
service receives key events to handle Settings/Notification and TV/Guide shortcuts;
it cannot retrieve screen content and can be disabled in Android Accessibility.
The original Fire OS 6 Keeper permissions remain limited to Android SDK 27 and below.

## Installation

The release APK requires Android 9 or newer. Install `dumbtv-home.apk` as an update
to the previous Grid edition signed by this fork, keeping the same package and
existing settings. Custom builds signed with a different key cannot update it.

Do not uninstall or reset the launcher just to update it. The source, build scripts
and checksums are included in the repository. The signing key, device settings,
screenshots and local development tools are not published.
