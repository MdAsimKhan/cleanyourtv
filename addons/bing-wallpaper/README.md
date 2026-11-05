# Bing Wallpaper

Daily 1080p photographic wallpaper from Microsoft Bing's official daily archive, updated automatically in the background for the [Clean Your TV](../../README.md) launcher.

For Clean Your TV it declares a [module card](../../docs/modules.md) ("Bing", caption "Daily Wallpaper" with an azure glow), where OK opens the preview screen with photo location/copyright info and a "Refresh Now" button.

## How it works

- **Direct Stream (Zero disk buildup)**: Downloads today's 1920x1080 JPEG directly into CleanYourTV's `wallpaper.jpg` in-place, creating 0 temporary files.
- **TV Boot Time Protection**: Does not execute on boot. It registers with Android's system `JobScheduler` and defers initial check until 10 minutes post-boot.
- **Background Hot-Reloading**: CleanYourTV updates its background texture smoothly without needing a TV reboot or Home restart.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Fetch today's image archive and 1080p photo from `bing.com`. |
| `RECEIVE_BOOT_COMPLETED` | Registers the periodic 24-hour JobScheduler task on device power-on. |
| `WRITE_SECURE_SETTINGS` | Write access to CleanYourTV's wallpaper content provider (`content://com.gathin.tvhome.theme/wallpaper.jpg`). |

No telemetry, no tracking, no background services, no user accounts.

## Build and install

```powershell
powershell -ExecutionPolicy Bypass -File addons\bing-wallpaper\build-and-install.ps1 -TvHost <tv-ip>
```
