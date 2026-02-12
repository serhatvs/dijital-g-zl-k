# MOBİL UYGULAMA - GPS HIZ VE MESAFE ÖLÇÜM

Bu klasör, GPS verilerini Bluetooth üzerinden Arduino'ya gönderen mobil uygulamayı içerir.

## Seçenekler

### 1. MIT App Inventor (Önerilen - No-Code)
- Kolay ve hızlı
- Programlama bilgisi gerektirmez
- Android için APK üretir
- **Rehber:** `MIT_APP_INVENTOR_REHBER.md`
- **Blocks Kodu:** `blocks_kodu.txt`

### 2. Web Bluetooth Demo (Test Amaçlı)
- Tarayıcı tabanlı test uygulaması
- Chrome/Edge Android gerekir
- **Dosya:** `web-bluetooth-demo.html`

### 3. React Native (İleri Seviye)
- Native performans
- iOS ve Android desteği
- **Klasör:** `react-native-app/` (opsiyonel)

## Hızlı Başlangıç

### MIT App Inventor ile:
1. https://appinventor.mit.edu adresine git
2. `MIT_APP_INVENTOR_REHBER.md` dosyasını takip et
3. Blocks'ları `blocks_kodu.txt` dosyasından kopyala
4. APK oluştur ve yükle

### Web Demo ile Test:
1. `web-bluetooth-demo.html` dosyasını Chrome/Edge'de aç
2. Bluetooth izni ver
3. HC-06 cihazını seç
4. GPS'i etkinleştir

## Dosya Yapısı

```
mobil-uygulama/
├── README.md                      # Bu dosya
├── MIT_APP_INVENTOR_REHBER.md    # Detaylı MIT App Inventor rehberi
├── blocks_kodu.txt               # App Inventor blocks pseudo-code
├── gps-bluetooth-app.aia         # MIT App Inventor proje dosyası
└── web-bluetooth-demo.html       # Test için web demo
```
