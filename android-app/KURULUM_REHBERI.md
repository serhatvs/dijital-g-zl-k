# ANDROID NATIVE UYGULAMA KURULUM REHBERİ

## 1. Android Studio Kurulumu

Android Studio indiriliyor:
```bash
snap install android-studio --classic
```

Kurulum tamamlandıktan sonra:
```bash
android-studio
```

## 2. İlk Çalıştırma (Setup Wizard)

1. Android Studio aç
2. "Standard" kurulum seçimi
3. SDK Components indirilecek (~3-4 GB):
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device

## 3. Projeyi Aç

```bash
# Android Studio'da:
File → Open → /home/serhat/vscode-workspace/dijital-gozluk/android-app
```

## 4. Gradle Sync

İlk açılışta otomatik olarak Gradle sync başlayacak:
- Dependencies indiriliyor
- Build tools hazırlanıyor
- ~5-10 dakika sürebilir (ilk kez)

## 5. Fiziksel Cihaz Bağlama

### USB Debugging Aç:
1. Telefon Ayarlar → Telefon Hakkında
2. "Yapı Numarası"na 7 kez tıkla (Developer Mode)
3. Ayarlar → Geliştirici Seçenekleri
4. "USB Debugging" aç

### Bilgisayara Bağla:
```bash
# ADB kontrol
adb devices

# Çıktı:
# List of devices attached
# ABC123XYZ	device
```

## 6. Uygulamayı Çalıştır

Android Studio'da:
1. Run → Run 'app' (Shift+F10)
2. Cihaz seçimi: Fiziksel cihazınızı seç
3. App yükleniyor ve açılıyor

## 7. İzinleri Ver

İlk açılışta izin isteyecek:
- ✅ Bluetooth
- ✅ Konum (GPS)

Her ikisine de "İzin Ver" de.

## 8. HC-05 Bağlantısı

1. Arduino'yu aç (HC-05 aktif)
2. Telefon Ayarlar → Bluetooth
3. HC-05'i eşleştir (PIN: 1234)
4. Uygulamaya dön
5. "Cihaz Seç" → HC-05 seç
6. "Bağlan"

## 9. Test

1. Dışarı çık (GPS sinyal için)
2. 30 saniye bekle (GPS fix)
3. Yürü veya araçla git
4. Arduino LCD'de veriyi gör!

## Dosya Yapısı

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/gps/speedmeasure/
│   │   │   ├── data/
│   │   │   │   ├── bluetooth/
│   │   │   │   │   └── BluetoothManager.kt  ✅
│   │   │   │   └── gps/
│   │   │   │       └── GPSManager.kt  ✅
│   │   │   └── presentation/
│   │   │       ├── MainActivity.kt  ✅
│   │   │       └── MainViewModel.kt  ✅
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml  ✅
│   │   │   └── values/
│   │   │       ├── strings.xml  ✅
│   │   │       └── colors.xml  ✅
│   │   └── AndroidManifest.xml  ✅
│   └── build.gradle  ✅
├── build.gradle  ✅
└── settings.gradle  ✅
```

## Kod Özellikleri

### ✅ BluetoothManager.kt
- HC-05 bağlantı yönetimi
- Eşleştirilmiş cihazları listele
- Veri gönderimi (SPEED:X,DIST:Y)
- Kotlin Flow ile reaktif

### ✅ GPSManager.kt
- FusedLocationProviderClient
- m/s → km/h dönüşümü
- Haversine mesafe hesaplama
- 1 saniye güncelleme

### ✅ MainViewModel.kt
- MVVM architecture
- State management
- Coroutines ile async işlemler
- Lifecycle-aware

### ✅ MainActivity.kt
- ViewBinding
- Permission handling (Android 12+ uyumlu)
- Material Design UI
- Real-time data updates

## APK Oluşturma

Debug APK:
```bash
cd android-app
./gradlew assembleDebug

# APK: app/build/outputs/apk/debug/app-debug.apk
```

Release APK (sign edilmiş):
```bash
./gradlew assembleRelease
```

## Özelleştirme

### Renkler:
`app/src/main/res/values/colors.xml`

### Metinler:
`app/src/main/res/values/strings.xml`

### Layout:
`app/src/main/res/layout/activity_main.xml`

### Veri formatı:
`GPSManager.kt` → `toBluetoothData()` fonksiyonu

## Sorun Giderme

### Gradle Sync Hatası:
```bash
./gradlew clean
./gradlew build
```

### ADB Cihazı Görmüyor:
```bash
adb kill-server
adb start-server
adb devices
```

### Bluetooth Bağlanmıyor:
- HC-05 eşleştirildi mi? (Ayarlar → Bluetooth)
- Arduino açık mı?
- Bluetooth izni verildi mi?

### GPS Çalışmıyor:
- Konum servisleri açık mı?
- Konum izni verildi mi?
- Açık alanda mısınız?

## Başarılar! 🚀

Android native geliştirme ile profesyonel bir uygulama oluşturdun.
MIT App Inventor'a göre daha fazla kontrol ve özelleştirme imkanı var.
