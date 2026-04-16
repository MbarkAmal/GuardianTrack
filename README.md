# GuardianTrack 🛡️

GuardianTrack is a robust safety monitoring application designed to detect falls using device sensors and alert emergency contacts automatically.

## ✨ Features
*   **Real-time Fall Detection**: Advanced accelerometer-based detection (§2.2 Spec compliant).
*   **Background Protection**: Reliable Foreground Service that stays active even when the app is closed.
*   **Auto-Resume on Boot**: Automatically restarts protection after a device reboot.
*   **Emergency Contacts**: Manage multiple safety contacts stored in a local encrypted database.
*   **Incident History**: Full log of detected incidents with date, time, and (optional) GPS data.
*   **Dynamic Sensitivity**: Adjust detection thresholds to match individual needs.

## 🛠️ Build Instructions
### Prerequisites
*   **Android Studio**: Hedgehog (2023.1.1) or newer.
*   **JDK**: Version 17 or higher.
*   **Device**: Android 8.0 (Oreo) minimum; Android 14 (UPSIDE_DOWN_CAKE) fully supported.

### Steps
1.  **Clone** the repository.
2.  Open the project in **Android Studio**.
3.  Ensure **Gradle** syncs successfully.
4.  Build the project: `Build > Make Project` (or `Ctrl+F9`).
5.  Run on a physical device (recommended for sensor testing).

## ⚙️ Configuration
### Permission Requirements
Upon first launch, the app will request:
- **Notifications**: To show monitoring status and alerts.
- **Location**: To pinpoint incident locations.

### Notification Channels
The app uses two distinct channels:
1.  **Monitor Status** (Low Priority): A silent notification showing the service is active.
2.  **Fall Alerts** (High Importance): A loud, vibrating notification that pops up when a fall is detected.

## 🏗️ Architecture
For deep technical details on the MVVM structure, Dependency Injection (Hilt), and the detection algorithm, please refer to the [Architecture Guide](file:///d:/GuardianTrack/Architecture_Guide.md).
