 
# ACT APP

![ACT App Screenshot](ACTAppPhoto/ACTAPP.png)

</br>

# Description

The ACT mobile application is a cutting-edge tool designed to detect and monitor nearby Wi-Fi networks, with a particular focus on identifying potentially contaminated areas. Developed as part of the broader ACT project, this app leverages advanced IoT technologies to scan for Wi-Fi networks, filter them based on predefined prefixes, and store the relevant data in a cache. One of its standout features is the ability to draw polygons on a map to define specific zones. When a zone is drawn, the app compares the SSIDs from the cached networks with the points within the polygon and triggers alerts if there is a match, indicating whether the zone is safe or contaminated. This real-time monitoring and alert system enhances user safety by providing timely notifications about potential risks in their environment.

</br>

# Technologies
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Android Studio](https://img.shields.io/badge/android%20studio-346ac1?style=for-the-badge&logo=android%20studio&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)

</br>


# Project Directory Structure for act_app

```
act_app/
|
|-- build.gradle.kts
|-- src/
    |
    |-- main/
    |   |-- java/
    |   |   |-- com/
    |   |       |-- example/
    |   |           |-- act_app/
    |   |               |-- ACTMapActivity.kt
    |   |               |-- AboutActivity.kt
    |   |               |-- ScreenReceiver.kt
    |   |               |-- SplashActivity.kt
    |   |               |-- WifiScanActivity.kt
    |   |               |-- WifiScanService.kt
    |   |
    |   |-- res/
    |       |-- drawable/
    |       |   |-- act.png
    |       |   |-- border.xml
    |       |   |-- button_background.xml
    |       |   |-- button_radius_corners.xml
    |       |   |-- circle_info_solid.xml
    |       |   |-- dialog_background.xml
    |       |   |-- dialog_background_contaminated.xml
    |       |   |-- dialog_background_safe.xml
    |       |   |-- gear_solid.xml
    |       |   |-- ic_launcher_background.xml
    |       |   |-- ic_launcher_foreground.xml
    |       |   |-- inria.png
    |       |   |-- keyboard_regular.xml
    |       |   |-- map_location_dot_solid.xml
    |       |   |-- rounded_button_background.xml
    |       |   |-- splash_background.xml
    |       |   |-- square_check_solid.xml
    |       |   |-- square_xmark_solid.xml
    |       |   |-- wifi_solid.xml
    |       |   |-- wifi_solid_green.xml
    |       |   |-- wifi_solid_yellow.xml
    |       |
    |       |-- layout/
    |       |   |-- act_map_activity_main.xml
    |       |   |-- activity_about.xml
    |       |   |-- activity_splash.xml
    |       |   |-- activity_wifi_scan.xml
    |       |   |-- dialog_alert.xml
    |       |   |-- dialog_settings.xml
    |       |   |-- list_item_custom.xml
    |       |   |-- nav_header.xml
    |       |
    |       |-- menu/
    |       |   |-- navigation_menu.xml
    |       |   |-- toolbar_menu.xml
    |       |
    |       |-- mipmap-anydpi-v26/
    |       |-- mipmap-hdpi/
    |       |-- mipmap-mdpi/
    |       |-- mipmap-xhdpi/
    |       |-- mipmap-xxhdpi/
    |       |-- mipmap-xxxhdpi/
    |       |
    |       |-- values/
    |           |-- colors.xml
    |           |-- strings.xml
    |           |-- themes.xml
    |
    |   |-- AndroidManifest.xml

```
</br>

# Permissions

The app requires the following permissions to function correctly:

- `INTERNET`: For accessing online resources.
- `ACCESS_FINE_LOCATION`: For precise location data.
- `ACCESS_COARSE_LOCATION`: For approximate location data.
- `ACCESS_WIFI_STATE`: To access Wi-Fi network information.
- `CHANGE_WIFI_STATE`: To enable or disable Wi-Fi.
- `FOREGROUND_SERVICE`: To run services in the foreground.
- `FOREGROUND_SERVICE_LOCATION`: To access location in the foreground.
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`: To interact with connected devices.
- `ACCESS_BACKGROUND_LOCATION`: To access location data in the background.
- `WAKE_LOCK`: To keep the device awake.
- `ACCESS_NETWORK_STATE`: To access information about network connectivity.
- `VIBRATE`: To control the vibrator.
- `POST_NOTIFICATIONS`: To display notifications.

</br>

# Activities

### SplashActivity

- **Purpose**: Serves as the launch screen of the application. It displays a splash screen with the app logo and transitions to the main activity after a short delay.
- **XML**: `activity_splash.xml`
- **Functionality**:
  - Displays a splash screen for a set duration (2 seconds).
  - Automatically transitions to the `ACTMapActivity` after the delay.
  - Provides a visually appealing introduction to the app.

### ACTMapActivity

- **Purpose**: The main activity that displays a map view with Wi-Fi network information and allows users to interact with the map.
- **XML**: `act_map_activity_main.xml`
- **Functionality**:
  - Integrates a WebView to display a map from a specified URL.
  - Utilizes JavaScript interfaces to communicate between the WebView and the Android app.
  - Retrieves and logs cached Wi-Fi networks.
  - Updates the UI with the latest scanned and cached networks.
  - Requests necessary permissions for Wi-Fi and location access.
  - Starts and stops the `WifiScanService` based on the activity lifecycle.
  - **Polygon Drawing and Alerts**:
    - Allows users to draw polygons on the map to define specific zones.
    - When a zone is drawn, the app compares the SSIDs from the cached networks with the points within the polygon.
    - Triggers an alert if there is a match, indicating whether the zone is a "Safe Zone" or a "Contaminated Zone" based on the matching SSIDs.
    - Provides visual feedback and notifications to inform users about the status of the defined zones.

### WifiScanActivity

- **Purpose**: Allows users to scan for Wi-Fi networks and view the results in a list.
- **XML**: `activity_wifi_scan.xml`
- **Functionality**:
  - Displays a list of all scanned Wi-Fi networks.
  - Updates the list in real-time as new networks are detected.
  - Displays a list of cached networks that match the specified SSID prefix.
  - Allows users to clear the cached networks.
  - Provides a settings dialog to customize scanning intervals, signal strength thresholds, and SSID prefixes.
  - Requests necessary permissions for Wi-Fi and location access.

### AboutActivity

- **Purpose**: Provides information about the application.
- **XML**: `activity_about.xml`
- **Functionality**:
  - Displays details about the app, such as version and developer information.
  - Offers a simple interface to learn more about the app's features and usage.

</br>

# Services

### WifiScanService

- **Purpose**: A foreground service that continuously scans for Wi-Fi networks in the background.
- **Functionality**:
  - Scans for Wi-Fi networks at regular intervals.
  - Caches network details and updates the UI through broadcast receivers.
  - Notifies users when new networks are cached.
  - Vibrates and plays a sound when new networks are detected.
  - Requires Wi-Fi and location permissions to function.

## Broadcast Receivers

### ScreenReceiver

- **Purpose**: Listens for screen on/off events to manage the `WifiScanService`.
- **Functionality**:
  - Starts the `WifiScanService` when the screen is turned off.
  - Stops the `WifiScanService` when the screen is turned on.
  - Helps in conserving battery by managing the service based on screen state.

</br>

# Steps to Install and Run the Application

### Clone the Repository
   Open a terminal or command prompt and run the following command to clone the repository:
```
git clone https://github.com/stevenessam/ACT_APP.git
```

### Open the Project in Android Studio

1. **Launch Android Studio**: Open Android Studio on your computer.
2. **Select "Open an existing Android Studio project"**: Choose this option from the welcome screen or from the File menu.
3. **Navigate to the cloned repository**: Browse to the directory where you cloned the `act_app` repository and select the `act_app` folder.

### Sync Project with Gradle Files

- Once the project is open in Android Studio, you may see a prompt to sync the project with Gradle files.
- Click on **"Sync Now"** to ensure all dependencies are downloaded and configured correctly. This step is crucial to avoid build errors related to missing dependencies.

### Configure the Emulator or Connect a Device

- **Using an Emulator**:
  - Create a new Android Virtual Device (AVD) with API level 21 or higher.
  - Configure the AVD settings according to your requirements and start the emulator.

- **Using a Physical Device**:
  - Enable **Developer Options** and **USB Debugging** on your Android device.
  - Connect the device to your computer using a USB cable.
  - Ensure the device is recognized by Android Studio.

### Build and Run the Application

1. **Click on the "Run" button**: Press the green play icon in Android Studio's toolbar.
2. **Select your device or emulator**: Choose your connected device or running emulator from the list of available devices.
3. **Build and Install**: The app will be compiled, and the APK will be installed on the selected device or emulator.

### Grant Necessary Permissions

- Upon the first launch, the app will request permissions for Wi-Fi and location access.
- Ensure these permissions are granted to enable full functionality, including Wi-Fi scanning and location-based features.

</br>

## Download the APK

You can download the APK file from the following link:

[Download ACTApp.apk](https://raw.githubusercontent.com/stevenessam/ACT_APP/main/ACT_APK/ACTApp.apk)


</br>

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
