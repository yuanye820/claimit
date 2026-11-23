# ClaimIt Android client

Java Android client for ClaimIt. `minSdk 26`, `compileSdk 35`, `targetSdk 35`.

## Features

- Register and log in with JWT; token stored in `SharedPreferences`.
- Create a list.
- Add an item with an integer total.
- Load items and display remaining quantities.
- Claim or release one unit.
- Show `409 Conflict` when another user took the last unit.
- Configurable API base URL; the default is `http://10.0.2.2:8000` for the Android emulator.

## Run

1. Start the backend from the repository root:

   ```bash
   docker compose up --build
   ```

   or:

   ```bash
   cd server
   uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```

2. Open this folder in Android Studio and let Gradle sync.
3. Start an emulator and run the app.
4. The emulator reaches the host through `10.0.2.2`. For a physical device, use the PC's LAN IP and allow port 8000 through the firewall.

## Build the APK

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- `android:usesCleartextTraffic="true"` is for local HTTP only; remove it when the API is behind HTTPS.
- Token storage is prototype-level. For production, use `EncryptedSharedPreferences` or the Android Keystore.
- `android.overridePathCheck=true` in `gradle.properties` is needed because the development path contains non-ASCII characters.
- The Kotlin stdlib `jdk7`/`jdk8` modules are excluded to avoid duplicate classes with `androidx.appcompat:appcompat:1.7.0`.
