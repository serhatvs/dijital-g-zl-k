# ANDROID NATIVE UYGULAMA - GPS HIZ ÖLÇER

## Proje Bilgileri

**Uygulama Adi:** GPS Hiz ve Mesafe Olcum  
**Paket Adi:** com.serhat.dijitalgozluk  
**Minimum SDK:** 24 (Android 7.0)  
**Target SDK:** 34 (Android 14)  
**Dil:** Kotlin  
**Mimari:** MVVM + Clean Architecture

## Özellikler

✅ Bluetooth Classic (HC-06) bağlantısı  
✅ GPS konum ve hız takibi  
✅ m/s → km/h dönüşümü  
✅ Haversine mesafe hesaplama  
✅ Gerçek zamanlı veri gönderimi  
✅ Material Design 3 UI  
✅ Permission handling  
✅ Lifecycle-aware components  

## Teknoloji Stack

- **UI:** Jetpack Compose / XML Views
- **Bluetooth:** Android Bluetooth API
- **GPS:** FusedLocationProviderClient
- **Permissions:** ActivityResultContracts
- **Coroutines:** Async işlemler için
- **ViewModel:** State management
- **Lifecycle:** Android Architecture Components

## Kurulum Gereksinimleri

1. Android Studio (en son stabil versiyon)
2. JDK 17 veya üzeri ✅ (Kurulu: OpenJDK 21)
3. Android SDK 34
4. Fiziksel Android cihaz (Bluetooth ve GPS için)

## Proje Yapisi

```
android-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/serhat/dijitalgozluk/
│   │   │   │   ├── data/
│   │   │   │   │   ├── bluetooth/
│   │   │   │   │   │   ├── BluetoothManager.kt
│   │   │   │   │   │   └── BluetoothDevice.kt
│   │   │   │   │   ├── gps/
│   │   │   │   │   │   ├── GPSManager.kt
│   │   │   │   │   │   └── LocationData.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── SpeedRepository.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── usecase/
│   │   │   │   │   │   ├── GetSpeedUseCase.kt
│   │   │   │   │   │   └── CalculateDistanceUseCase.kt
│   │   │   │   │   └── model/
│   │   │   │   │       └── SpeedData.kt
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── SpeedScreen.kt
│   │   │   │   │       └── BluetoothScreen.kt
│   │   │   │   └── util/
│   │   │   │       ├── PermissionManager.kt
│   │   │   │       └── Extensions.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
└── build.gradle.kts
```

## Kurulum Adımları

### 1. Android Studio Kurulumu
```bash
# Snap ile kurulum (yükleniyor...)
snap install android-studio --classic

# Veya manuel:
# https://developer.android.com/studio
```

### 2. Proje Olusturma
1. Android Studio'yu ac
2. New Project → Empty Activity
3. Name: GPS Speed Measure
4. Package: com.serhat.dijitalgozluk
5. Language: Kotlin
6. Minimum SDK: API 24 (Android 7.0)

### 3. Dependencies Ekleme
build.gradle.kts (app):
```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // UI
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Location
    implementation("com.google.android.gms:play-services-location:21.1.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Compose (opsiyonel)
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.activity:activity-compose:1.8.2")
}
```

## Hızlı Başlangıç

Android Studio kurulumu tamamlandıktan sonra:

```bash
cd /home/serhat/vscode-workspace/dijital-gozluk/android-app
./gradlew assembleDebug
```

## Dokümantasyon

- **[ANDROID_DEV_GUIDE.md](ANDROID_DEV_GUIDE.md)** - Detaylı geliştirme rehberi
- **[BLUETOOTH_API.md](BLUETOOTH_API.md)** - Bluetooth implementasyonu
- **[GPS_API.md](GPS_API.md)** - GPS implementasyonu
- **[PERMISSIONS.md](PERMISSIONS.md)** - Runtime permissions

## Test Etme

### Fiziksel Cihazda
1. USB Debugging aç (Developer Options)
2. USB kablosu ile bağla
3. Run 'app' (Shift+F10)

### Bluetooth Test
1. HC-06'yı Arduino'ya bağla
2. Telefonda Bluetooth aç ve eşleştir
3. Uygulamada cihazı seç ve bağlan

## Veri Formatı

Arduino'ya gönderilen veri:
```
SPEED:45.50,DIST:1.32\n
```

Bluetooth Serial: 9600 baud rate

## Performans

- GPS güncelleme: 1 saniye
- Bluetooth gönderim: 1 saniye
- UI güncelleme: Her GPS değişiminde
- Bellek kullanımı: ~50-80 MB

## İzinler

AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Kaynaklar

- Android Bluetooth Guide: https://developer.android.com/guide/topics/connectivity/bluetooth
- Location Services: https://developer.android.com/training/location
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
