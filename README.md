# App_teste_captacao

This is a new React Native project, bootstrapped using @react-native-community/cli.
📡 Project Description

This prototype is designed to be part of a production app and focuses on capturing cellular network quality in the background on Android devices.

Every 15 minutes, a background task collects:

    📶 RSRP (Reference Signal Received Power)

    📶 RSRQ (Reference Signal Received Quality)

    🛰️ Latitude and Longitude

    🧩 cellId

# After a defined number of samples, the data is exported for further analysis or external use.
🔍 Native Android Stack (Kotlin)

    WorkManager — background job scheduling

    TelephonyManager — mobile signal metrics (4G LTE / 5G NR)

    FusedLocationProviderClient — GPS coordinates

    OkHttp — optional for API submission

    Tasks.await() — for handling async location retrieval

# This Kotlin logic lives in NetworkMonitoringWorker.kt.
🚀 Getting Started

    Note: Before starting, follow the official React Native Environment Setup up to the "Creating a new application" step.

1. Start Metro Bundler

npm start
# or
yarn start

2. Run the App

In a new terminal:
Android

npm run android
# or
yarn android

iOS (not used in this prototype)

npm run ios
# or
yarn ios

    ✅ Ensure your emulator/device is properly set up.

🧪 Modify the App

    Edit the App.tsx file.

    For Android: press <kbd>R</kbd> twice or open the Developer Menu (Cmd+M or Ctrl+M).

    For iOS: press Cmd+R in the Simulator.

🔧 Permissions Required

Add the following permissions to AndroidManifest.xml:

<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28"/>

