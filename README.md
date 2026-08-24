# HK Weather - Android App

A beautiful weather app for Hong Kong using the Hong Kong Observatory (HKO) OpenData API and Google Maps.

## Features

- **Current Location on Map** — Shows your current location on a Google Map centered on Hong Kong
- **Current Temperature** — Real-time temperature from HKO weather stations with daily high/low
- **Humidity** — Current humidity levels from HKO
- **Wind Speed & Direction** — Live wind data with compass direction
- **Rainfall Information** — Current rainfall across Hong Kong districts
- **Coming Rain Alerts** — Notification when rain is detected nearby
- **9-Day Weather Forecast** — Extended forecast from HKO with daily weather, temps, and humidity
- **Weather Warnings** — Active HKO weather warnings banner

## Quick Start

### Prerequisites

- [Android Studio Hedgehog](https://developer.android.com/studio) or newer
- JDK 17
- Android SDK with API 34

### 1. Clone & Open

```bash
git clone <your-repo-url> HKWeather
cd HKWeather
```

Open the project in Android Studio.

### 2. Set Up Local SDK Path

Copy `local.properties.template` to `local.properties` and update the SDK path:

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
```

> Replace the path with your actual Android SDK location.

### 3. Get a Google Maps API Key

The app needs a Google Maps API key to display the map.

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select an existing one)
3. Enable **Maps SDK for Android**:
   - Go to **APIs & Services** → **Library**
   - Search for "Maps SDK for Android" and enable it
4. Create an API key:
   - Go to **APIs & Services** → **Credentials**
   - Click **Create Credentials** → **API Key**
   - Copy the generated key
5. (Recommended) Restrict the key:
   - Click on the API key → **Application restrictions** → **Android apps**
   - Add your package name: `com.hkweather.app`
   - Add your SHA-1 signing certificate fingerprint:
     ```bash
     # Debug key fingerprint:
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
     
     # Release key fingerprint:
     keytool -list -v -keystore hkweather-release-key.jks -alias hkweather -storepass hkweather123
     ```

6. Paste your key into `gradle.properties`:

```properties
GOOGLE_MAPS_API_KEY=AIzaSyYourActualApiKeyHere
```

### 4. Build & Run

**Debug build:**
```bash
./gradlew assembleDebug
```

**Install on device:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/release/app-release.apk
```

**Release build (signed):**
```bash
./gradlew assembleRelease
```
The signed APK will be at `app/build/outputs/apk/release/app-release.apk`.

## App Signing

A release keystore is included in the project root:

| Field | Value |
|---|---|
| Keystore file | `hkweather-release-key.jks` |
| Keystore password | `hkweather123` |
| Key alias | `hkweather` |
| Key password | `hkweather123` |
| Algorithm | RSA 2048-bit (PKCS12) |
| Validity | 10,000 days (~27 years) |
| Signature scheme | APK Signature Scheme v2 |

> **For production:** Replace this keystore with your own and keep it safe. Never commit real keystores to version control.

### Build Commands

```bash
# Debug APK (unsigned) — for development
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (signed) — for distribution
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Verify APK Signature

```bash
# Using apksigner (in Android SDK build-tools)
C:\Android\Sdk\build-tools\35.0.0\apksigner.bat verify --verbose app/build/outputs/apk/release/app-release.apk
```

### Get Certificate Fingerprints

```bash
# Release key SHA-1 (for Google Maps API restriction)
keytool -list -v -keystore hkweather-release-key.jks -alias hkweather -storepass hkweather123

# Debug key SHA-1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

## Weather Data

Uses the free [HKO OpenData API](https://www.hko.gov.hk/en/abouthko/opendata_intro.htm) — no API key required.

### API Endpoints Used

| Endpoint | Data Type | Description |
|---|---|---|
| `rhrread` | Current weather | Temperature, humidity, wind, rainfall, UV index |
| `fnd` | Forecast | 9-day weather forecast |
| `warningInfo` | Warnings | Active weather warnings |
| `rainfall` | Rainfall | Rainfall data across HK districts |

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **DI:** Hilt (Dagger)
- **Networking:** Retrofit + OkHttp + Gson
- **Maps:** Google Maps SDK for Android
- **Async:** Kotlin Coroutines
- **Location:** Google Play Services Location

## Project Structure

```
HKWeather/
├── build.gradle.kts                    # Root build file
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # API keys & config
├── gradlew.bat                         # Gradle wrapper (Windows)
├── gradle/wrapper/                     # Gradle wrapper config
├── local.properties.template           # SDK path template
├── hkweather-release-key.jks           # Release signing key
└── app/
    ├── build.gradle.kts                # App module build
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/hkweather/app/
        │   ├── HKWeatherApplication.kt
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── api/HKOApiService.kt
        │   │   ├── model/WeatherModels.kt
        │   │   └── repository/WeatherRepository.kt
        │   ├── di/
        │   │   ├── NetworkModule.kt
        │   │   └── LocationModule.kt
        │   └── ui/
        │       ├── theme/Theme.kt
        │       ├── viewmodel/WeatherViewModel.kt
        │       └── screen/WeatherScreen.kt
        └── res/                         # Icons, themes, strings
```

## Troubleshooting

- **Map not loading:** Verify your Google Maps API key is correct and the Maps SDK for Android is enabled
- **Location not working:** Make sure location permissions are granted in app settings
- **Weather data not loading:** Check internet connection; HKO API is accessible globally
- **Build fails:** Ensure `JAVA_HOME` is set and points to JDK 17

## License

MIT
