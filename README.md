# 🚗 Dijital Gözlük - GPS Hız ve Mesafe Ölçüm Sistemi

Arduino Uno tabanlı GPS hız ölçüm sistemi. Mobil uygulama ile GPS verilerini Bluetooth üzerinden Arduino'ya göndererek LCD ekranda görüntüler.

## 📋 Proje Özellikleri

- **Arduino Uno** + HC-06 Bluetooth + 16x2 LCD I2C
- **Android Native App** (Kotlin MVVM mimarisi)
- GPS hız takibi (m/s → km/h dönüşümü)
- Haversine formülü ile mesafe hesaplama
- Bluetooth SPP ile gerçek zamanlı veri iletişimi
- Her 1 saniyede veri güncelleme

## 🔧 Donanım Gereksinimleri

### Arduino Sistemi
- Arduino Uno (veya klon CH340 USB chip)
- HC-06 Bluetooth modülü
- 16x2 LCD ekran (I2C adaptör ile)
- Breadboard ve jumper kablolar
- 5V güç kaynağı

### Mobil Cihaz
- Android 7.0+ (API 24)
- GPS özelliği
- Bluetooth Classic desteği

## 📦 Kurulum

### 1. Arduino Kurulumu

```bash
# Arduino IDE veya CLI ile yükle
cd arduino_kod
# arduino_kod.ino dosyasını Arduino'ya yükle

# Veya otomatik kurulum scripti:
chmod +x klon_arduino_kurulum.sh
./klon_arduino_kurulum.sh
```

**Gerekli Kütüphaneler:**
- LiquidCrystal_I2C (1.1.2+)
- SoftwareSerial (built-in)

### 2. Android App Kurulumu

```bash
cd android-app
./gradlew assembleDebug

# APK: app/build/outputs/apk/debug/app-debug.apk
# Telefona kur
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🎯 Kullanım

1. **Arduino'yu güç ver** → LCD'de "Bluetooth Bekliyor..." mesajı görünür
2. **Android uygulamayı aç** → İzinleri ver (Bluetooth + GPS)
3. **HC-06'ye bağlan** → Eşleştirilmiş cihazlardan seç
4. **GPS sinyali al** → Hareket ettikçe hız ve mesafe gösterilir
5. **LCD ekranda göster** → Arduino LCD'de anlık veri

## 📡 Veri Protokolü

```
Format: SPEED:XX.XX,DIST:YY.YY\n
Örnek: SPEED:45.50,DIST:1.32\n

- Hız: km/h (2 ondalık)
- Mesafe: km (2 ondalık)
- Güncelleme: 1 saniye
- Baud rate: 9600
```

## 📚 Dokümantasyon

- [PROJE_REHBERI.md](PROJE_REHBERI.md) - Detaylı proje dokümantasyonu, formüller
- [MONTAJ_REHBERI.md](MONTAJ_REHBERI.md) - Donanım montaj adımları, pin bağlantıları
- [KLON_ARDUINO_KURULUM.md](KLON_ARDUINO_KURULUM.md) - Klon Arduino (CH340) kurulum rehberi
- [android-app/README.md](android-app/README.md) - Android uygulama dokümantasyonu

## 🏗️ Proje Yapısı

```
dijital-gozluk/
├── arduino_kod/
│   └── arduino_kod.ino          # Arduino firmware
├── android-app/
│   ├── app/src/main/java/com/serhat/dijitalgozluk/
│   │   ├── data/
│   │   │   ├── bluetooth/       # Bluetooth yönetimi
│   │   │   └── gps/             # GPS tracking
│   │   └── presentation/        # UI katmanı
│   └── build.gradle
├── PROJE_REHBERI.md             # Ana dokümantasyon
├── MONTAJ_REHBERI.md            # Montaj rehberi
└── README.md                    # Bu dosya
```

## 🧪 Test Senaryoları

1. **Statik Test**: Hız = 0.00 km/h, mesafe artmamalı
2. **Yürüme Testi**: 4-6 km/h hız aralığı
3. **Araç Testi**: 30-50 km/h, araç gösterge ile karşılaştır
4. **Bağlantı Testi**: Bluetooth kesme/yeniden bağlanma
5. **GPS Kayıp**: Tünel/iç mekan davranışı

## 🎓 Eğitim Amaçlı

Bu proje bilgisayar mühendisliği öğrencileri için geliştirilmiştir:
- Embedded systems (Arduino programlama)
- Mobile development (Android Kotlin)
- Wireless communication (Bluetooth SPP)
- Sensor integration (GPS)
- MVVM architecture pattern
- Real-time data processing

## 📄 Lisans

MIT License - Eğitim amaçlı kullanım için ücretsiz

## 🤝 Katkıda Bulunma

1. Fork et
2. Feature branch oluştur (`git checkout -b feature/amazing`)
3. Commit et (`git commit -m 'Add amazing feature'`)
4. Push et (`git push origin feature/amazing`)
5. Pull Request aç

## 📞 İletişim

**Geliştirici**: serhatvs  
**GitHub**: https://github.com/serhatvs/dijital-g-zl-k

---

⭐ Faydalı bulduysanız projeye yıldız vermeyi unutmayın!
