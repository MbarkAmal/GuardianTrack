# Architecture Guide - GuardianTrack

GuardianTrack is built using modern Android development practices, emphasizing separation of concerns, testability, and reliability for its safety-critical nature.

## 🏗 Architecture Pattern: MVVM
The project follows the **Model-View-ViewModel** pattern:
*   **View (Activities/Fragments)**: Responsible for UI rendering and observing LiveData/Flow from ViewModels.
*   **ViewModel**: Holds UI state and handles business logic, interacting with the Repository layer.
*   **Model/Repository**: Centralized data management. The repository decides whether to fetch data from the local database or other sources.

## 📦 Package Structure
- `com.example.guardiantrack`
    - `data`: Database entities, DAOs, Room Database, and Repository implementations.
    - `data.model`: Data classes and Room entities.
    - `data.repository`: Business logic for data access.
    - `di`: Dagger Hilt modules for Dependency Injection.
    - `provider`: ContentProvider for exposing emergency contacts safely.
    - `receiver`: BroadcastReceivers for `BOOT_COMPLETED` and `BATTERY_LOW`.
    - `service`: Foreground Services for real-time sensor surveillance.
    - `ui`: Fragments and Adapters for Dashboard, History, and Settings.
    - `util`: Helper classes (e.g., historians, exporters).
    - `viewmodel`: ViewModels for different screens.
    - `worker`: WorkManager workers for handling background tasks (like restarting the service on boot).

## ⚡ Key Technologies
- **Language**: Kotlin with Coroutines and Flow.
- **Dependency Injection**: [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for clean decoupled components.
- **Location Services**: [Google Play Services Location](https://developers.google.com/android/guides/setup) for accurate coordinate tracking.
- **Database**: [Room](https://developer.android.com/training/data-storage/room) for persistent incident history and contacts.
- **Preferences**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for settings (Dark mode, sensitivity).
- **Background Processing**:
    - **Foreground Service**: Ensures the app stays active in the background for real-time sensor monitoring.
    - **WorkManager**: Used for "Expedited" work to safely start the service after a device reboot.

## 🏃 Fall Detection Algorithm (§2.2)
The detection logic is implemented in `SurveillanceService` and operates in two sequential phases:
1.  **Phase 1 (Free-fall)**: Magnitude (`sqrt(x²+y²+z²)`) drops below **3 m/s²** for strictly **> 100ms**.
2.  **Phase 2 (Impact)**: Magnitude spikes above the **Sensitivity Threshold** (default 15 m/s²) within **200ms** of the free-fall end.

### Sensor Optimized Threading (§4.2)
To ensure the main UI thread remains fluid, sensor processing is offloaded to a dedicated **HandlerThread** called `SensorThread`. This ensures high-frequency sensor events are processed exactly on time without UI lag.

## 📍 Geolocation Strategy (§4.1)
The app integrates `FusedLocationProviderClient` to attach precise coordinates to every incident.
- **Strategy**: On every detection (Fall or Battery), the app attempts to fetch the current location with high accuracy.
- **Sentinel Values**: If permission is denied or the GPS is disabled, the system follows a "safe-fail" strategy, recording **`0.0 / 0.0`** as coordinates. This allows the incident to be recorded for time/type even when location is unavailable.
- **User Guidance**: A rationale dialog is displayed if permissions are missing, explaining the safety benefits of enabling location.
- `FOREGROUND_SERVICE`: Essential for background execution.
- `FOREGROUND_SERVICE_HEALTH`: Required for sensor access in background (Android 14+).
- `RECEIVE_BOOT_COMPLETED`: Allows the app to auto-restart protection when the phone reboots.
- `POST_NOTIFICATIONS`: Required to show alerts and the monitoring status (Android 13+).
- `ACCESS_FINE_LOCATION`: Used to record GPS coordinates during an incident.
