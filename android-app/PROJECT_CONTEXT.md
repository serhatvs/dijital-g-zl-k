# Digital Glasses - GPS Speed Measurement Android App

## Project Overview
Android application that reads GPS speed and distance, then transmits data via Bluetooth to Arduino Uno with HC-06 module. Arduino displays the data on 16x2 LCD screen.

## Technical Requirements

### Hardware Communication
- **Bluetooth Module:** HC-06 (SPP - Serial Port Profile)
- **UUID:** `00001101-0000-1000-8000-00805F9B34FB` (Standard SPP UUID)
- **Baud Rate:** 9600
- **Arduino Board:** Arduino Uno (with SoftwareSerial on pins 10, 11)

### Data Protocol
**Format:** `SPEED:XX.XX,DIST:YY.YY\n`
- Speed in km/h (2 decimal places)
- Distance in kilometers (2 decimal places)
- Newline terminated
- Example: `SPEED:45.50,DIST:1.32\n`

**Update Frequency:** 1 second intervals

### GPS Requirements
- Access to device GPS sensor
- Calculate speed from GPS coordinates (convert m/s to km/h)
- Calculate cumulative distance using Haversine formula
- Handle GPS signal loss gracefully

## Architecture

### MVVM Pattern
- **MainActivity:** UI layer, displays speed/distance, Bluetooth connection status
- **SpeedViewModel:** Business logic, manages LiveData for UI updates
- **BluetoothManager:** Handles HC-06 connection, pairing, data transmission
- **GPSManager:** Manages location updates, speed calculation, distance tracking

### Key Components

#### BluetoothManager.kt
```kotlin
// Responsibilities:
// - Scan for HC-06 devices
// - Connect using SPP UUID
// - Send formatted string data
// - Monitor connection state
// - Handle reconnection logic
```

#### GPSManager.kt
```kotlin
// Responsibilities:
// - Request location permissions (ACCESS_FINE_LOCATION)
// - Register for GPS updates (1 second interval)
// - Calculate speed from LocationProvider (convert m/s to km/h)
// - Calculate distance between coordinates (Haversine formula)
// - Accumulate total distance traveled
```

#### SpeedViewModel.kt
```kotlin
// LiveData properties:
// - currentSpeed: Double (km/h)
// - totalDistance: Double (km)
// - bluetoothConnected: Boolean
// - gpsEnabled: Boolean
// 
// Methods:
// - startTracking()
// - stopTracking()
// - resetDistance()
// - connectBluetooth()
// - disconnectBluetooth()
```

## Required Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Dependencies (build.gradle.kts)
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
implementation("com.google.android.gms:play-services-location:21.0.1")
```

## UI Requirements

### MainActivity Layout
- **Speed Display:** Large TextView showing current speed in km/h
- **Distance Display:** TextView showing cumulative distance in km
- **Bluetooth Status:** Indicator (connected/disconnected)
- **GPS Status:** Indicator (enabled/disabled)
- **Connect Button:** Pair and connect to HC-06
- **Reset Button:** Reset distance counter to 0
- **Start/Stop Button:** Begin/end tracking

### Design Notes
- Use large, readable fonts for speed display (visible while moving)
- Keep screen on during tracking
- Show toast messages for connection status
- Request runtime permissions for Android 6.0+

## Testing Scenarios
1. **Static Test:** Speed = 0.00 km/h, distance should not increase
2. **Walking Test:** Speed ~4-6 km/h
3. **Vehicle Test:** Speed 30-50 km/h, verify accuracy against car speedometer
4. **Connection Test:** Disconnect/reconnect Bluetooth while tracking
5. **GPS Loss:** Handle tunnels, indoor areas gracefully

## Haversine Distance Formula
```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2) * sin(dLat/2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon/2) * sin(dLon/2)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    return R * c
}
```

## Error Handling
- Bluetooth not available: Show error dialog
- GPS disabled: Prompt user to enable location
- No HC-06 found: List available devices, manual selection
- Connection lost: Auto-reconnect with 3 retry attempts
- Permission denied: Explain why permissions are needed

## Package Structure
```
com.serhat.dijitalgozluk/
├── MainActivity.kt
├── managers/
│   ├── BluetoothManager.kt
│   └── GPSManager.kt
└── viewmodels/
    └── SpeedViewModel.kt
```

## Student Project Context
This is a computer engineering student project demonstrating:
- Mobile sensor integration (GPS)
- Wireless communication (Bluetooth SPP)
- Embedded systems integration (Arduino)
- Real-time data processing
- MVVM architecture in Android

**Target Device:** Android phone with GPS and Bluetooth 2.0+
**Deployment:** APK for manual installation (non-Play Store)
