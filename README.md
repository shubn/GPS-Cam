# GPS Map Camera (Android)

A native Kotlin Android camera app with live CameraX preview, high-accuracy GPS capture, reverse-geocoded address, date/time overlay, GPS watermark burned into saved JPEG photos, map handoff, and an in-app photo gallery. No company branding or ads are included.

## Open and build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this `GPSMapCamera` folder in Android Studio.
3. Allow Gradle sync and dependency downloads.
4. Connect an Android phone (Android 8.0 / API 26 or newer), enable Developer options and USB debugging.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions and privacy
- Camera permission is used for live preview and photo capture.
- Fine/coarse location is used to stamp the current location on the photo.
- Location is processed on-device for the photo stamp; no account or server is used by this project.
- Address lookup uses Android Geocoder, which may rely on the device's configured geocoding service and may not work offline.
- The Map button opens an installed map app using the current coordinates.
- Photos are saved to `Pictures/GPSMapCamera` in the device media gallery.

## Notes
- GPS accuracy depends on device hardware, permissions, sky visibility, and location settings. A location can be unavailable indoors.
- The app stamps the latest location fix available at capture time. Refresh location by reopening the app; the app also refreshes after each photo.
- This source project is provided for development and testing. It has not been compiled or device-tested in this environment because an Android SDK/Gradle build environment is not installed here.
