# Capsule Commons prototype

Standalone prototype published to https://github.com/NorthernLadd/Capsule_Proto.
The original repository is separate. No API key required.
Application ID: com.example.capsule.prototype (coexists with original).

Run from Android Studio or ./gradlew.bat assembleDebug.
The Home and Map destinations show the Commons prototype list; this version does not add a map SDK.
Other original screens remain demonstrations.

1. Tap Find nearby photos and grant location permission.
2. Fetch up to 10 geotagged Commons files within 2 km.
3. Fetch details in sequential batches of 5.
4. Accept JPEG/PNG thumbnails with geographic coordinates, creator, source and a supported CC license.
5. Review the source and select Create discovery capsule.
6. Wait one minute, approach within 161 m, and refresh location. Opening uses a fresh fix and an accuracy margin.
7. Opened images and capsule unlock timestamps persist locally across restarts.

Emulator: Extended Controls > Location > set a position near a photographed landmark.
No demo coordinates silently replace real device location.
Approximate fixes support browsing but generally cannot satisfy opening accuracy.
Source links are always available, so this is an exploration mechanic, not protected content.
Coordinates and licensing are contributor supplied: review source files before visiting.
No server authentication or GPS anti-spoofing is implemented. Local JSON in SharedPreferences
is intended for a small prototype dataset. Clearing app data deletes this local history.
Commons can return empty results, unsupported files or missing credits.
Use a project contact URL in CommonsClient's User-Agent before distribution.
