# GPS Map Camera (Android)

A native Kotlin Android camera app with live CameraX preview, GPS coordinates, reverse-geocoded address, date/time overlay, GPS watermark burned into saved JPEG photos, map handoff, and an in-app photo gallery. No company branding or ads are included.

## Build the APK online (GitHub Actions)

This repository includes an automated cloud build workflow at `.github/workflows/build-apk.yml`.

1. Sign in to [GitHub](https://github.com/) and create a **public or private** repository named `GPSMapCamera`.
2. Extract the source ZIP on your computer.
3. Upload the **contents** of the `GPSMapCamera` folder to the root of your GitHub repository. Make sure `.github/workflows/build-apk.yml` is included. If GitHub's web uploader hides `.github`, create the workflow file in GitHub using **Add file → Create new file** and enter the path `.github/workflows/build-apk.yml`.
4. Commit the files to the `main` branch.
5. Open the repository's **Actions** tab and select **Build GPS Map Camera APK**. If the workflow has not started from the upload, choose **Run workflow → Run workflow**.
6. Wait for the run to finish with a green check mark. Open the completed run and download the **GPS-Map-Camera-APK** artifact.
7. Extract the downloaded artifact ZIP. It contains `app-debug.apk`. Transfer it to your Android phone and install it. You may need to allow installation from that source in Android settings.

The workflow uses Java 17 and Gradle 8.9, builds the debug APK with `assembleDebug`, and keeps the downloadable artifact for 14 days. Every push to `main` also triggers a build.

## Build locally with Android Studio

1. Install [Android Studio](https://developer.android.com/studio) and Android SDK Platform 35.
2. Open this `GPSMapCamera` folder in Android Studio.
3. Allow Gradle sync and dependency downloads.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions and privacy

- Camera permission is used for live preview and photo capture.
- Fine/coarse location is used to stamp the current location on the photo.
- Location is processed on-device for the photo stamp; no account or app server is used by this project.
- Address lookup uses Android Geocoder, which may rely on the device's configured geocoding service and may not work offline.
- The Map button opens an installed map app using the current coordinates.
- Photos are saved to `Pictures/GPSMapCamera` in the device media gallery.

## Notes

- GPS accuracy depends on device hardware, permissions, sky visibility, and location settings. A location can be unavailable indoors.
- The app stamps the latest location fix available at capture time. Refresh location by reopening the app; the app also refreshes after each photo.
- This is a debug build workflow for testing, not a signed release / Play Store publishing pipeline.
- The project has not yet been verified on a physical Android device. Test camera, location, and photo saving on your phone before relying on it for field records.
