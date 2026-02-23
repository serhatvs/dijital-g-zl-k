# SABAH_DEVAM.md'den Eklenenler

## 23 Şubat 2026 (Bugün)
- [x] KVM ve dialout grup kontrolü (login/logout veya restart gerekebilir)
- [x] Arduino kodunu yükle: RAM optimizasyonu, GPS simülasyonu, son konum ekranda kalıyor ([arduino_kod/arduino_kod.ino](arduino_kod/arduino_kod.ino))
- [x] Android APK oluştur ve emulator/test et ([android-app/app/build/intermediates/apk/debug/app-debug.apk](android-app/app/build/intermediates/apk/debug/app-debug.apk))
- [x] GitHub commit ve push işlemleri

## 24 Şubat 2026
- [x] APK'yı telefona yükle (Bluetooth/Email/WhatsApp ile aktar)
- [x] Telefonda yükleme ve izinlerin verilmesi

## 25 Şubat 2026
- [ ] Gerçek test: Arduino hazır, LCD "BT Bekleniyor..." gösteriyor
- [ ] Telefonda uygulama aç, Bluetooth ve GPS izinlerini ver
- [ ] HC-06 eşleştir, "Cihaz Seç" ve "Bağlan" işlemleri
- [ ] Telefonu hareket ettir, LCD'de hız ve mesafe gör
- [ ] Veri formatı: SPEED:X.X,DIST:Y.YY,LAT:Z,LON:W

## 26 Şubat 2026
- [ ] Arduino compile & upload işlemleri
- [ ] Emulator başlatma ve test ([~/Android/Sdk/emulator/emulator -avd Pixel_5 ...](SABAH_DEVAM.md))

## 27 Şubat 2026
- [ ] Bilinen sorunların kontrolü: KVM izni, HC-06 baud rate, LCD I2C address, RAM kullanımı
- [ ] HC-05 bağlantı ve voltage divider kontrolü

## 28 Şubat 2026
- [ ] Release APK oluştur (signed)
- [ ] Play Store hazırlığı
- [ ] Arduino kutu tasarımı (3D print)
- [ ] Güç kaynağı seçimi (powerbank/batarya)
- [ ] Montaj rehberi fotoğrafları

## 29 Şubat 2026
- [ ] IMU verisi ile EKF entegrasyonu
- [ ] EKF parametrelerinin optimize edilmesi
- [ ] EKF ve Kalman karşılaştırmalı testler
- [ ] Mobil uygulamada filtre seçimi arayüzü
- [ ] Dokümantasyon ve rehber güncellemesi
- [ ] Gerçek saha verisi ile performans analizi
- [ ] PCB tasarımı ve üretim dosyalarının hazırlanması
- [ ] PCB bileşen listesi (BOM) oluşturulması
- [ ] PCB test ve doğrulama planı hazırlanması

---

# Dijital Gözlük Proje Checklist (Günlük Plan)

## 23 Şubat 2026 (Bugün)
- [ ] Android Studio kurulumu ([android-app/KURULUM_REHBERI.md](android-app/KURULUM_REHBERI.md))
- [ ] Proje oluşturma ve ilk yapılandırma
- [ ] Gerekli bağımlılıkların eklenmesi ve Gradle sync ([android-app/README.md](android-app/README.md))
- [ ] Fiziksel cihaz bağlantısı ve izinlerin verilmesi

## 24 Şubat 2026
- [ ] Bluetooth modülünün (HC-06) Arduino'ya bağlanması ([HC06_BAUD_RATE_AYARLAMA.md](HC06_BAUD_RATE_AYARLAMA.md), [MONTAJ_REHBERI.md](MONTAJ_REHBERI.md))
- [ ] Arduino kodunun yüklenmesi ([arduino_kod/arduino_kod.ino](arduino_kod/arduino_kod.ino))
- [ ] LCD ekran ve diğer sensörlerin montajı ([MONTAJ_REHBERI.md](MONTAJ_REHBERI.md))
- [ ] HC-06 baud rate ayarının yapılması ve test edilmesi

## 25 Şubat 2026
- [ ] Bluetooth bağlantı testleri ve veri formatı kontrolü ([PROJE_REHBERI.md](PROJE_REHBERI.md))
- [ ] Bluetooth bağlantısının mobil uygulama ile test edilmesi
- [ ] LCD ekranda bağlantı durumunun doğrulanması

## 26 Şubat 2026
- [ ] GPS optimizasyonu ve testleri ([GPS_OPTIMIZASYON_REHBERI.md](GPS_OPTIMIZASYON_REHBERI.md))
- [ ] GPS sinyalinin alınması ve doğrulanması
- [ ] GPS smoothing algoritmasının test edilmesi

## 27 Şubat 2026
- [ ] Mobil uygulama geliştirme (MIT App Inventor veya Android Native) ([mobil-uygulama/MIT_APP_INVENTOR_REHBER.md](mobil-uygulama/MIT_APP_INVENTOR_REHBER.md), [android-app/README.md](android-app/README.md))
- [ ] Uygulama arayüzünün hazırlanması
- [ ] Bluetooth ve GPS entegrasyonunun kodlanması

## 28 Şubat 2026
- [ ] Uygulama test senaryoları ve gerçek dünya testleri ([README.md](README.md), [PROJE_DURUM_RAPORU.md](PROJE_DURUM_RAPORU.md))
- [ ] Statik test: Hız = 0.00 km/h, mesafe artmamalı
- [ ] Yürüme testi: 4-6 km/h hız aralığı
- [ ] Araç testi: 30-50 km/h, araç gösterge ile karşılaştır
- [ ] Bağlantı testi: Bluetooth kesme/yeniden bağlanma
- [ ] GPS kayıp senaryosu: tünel/iç mekan davranışı

## 29 Şubat 2026
- [ ] LCD ekranda veri doğrulama
- [ ] Son raporun hazırlanması ve akademik özet ([PROJE_REHBERI.md](PROJE_REHBERI.md))
- [ ] Geliştirme ve iyileştirme adımları (SD kart, web dashboard, hız limiti uyarı, vb.) ([PROJE_DURUM_RAPORU.md](PROJE_DURUM_RAPORU.md))
- [ ] Son test ve proje kapanışı

---

### Kaynaklar
- [PROJE_REHBERI.md](PROJE_REHBERI.md)
- [MONTAJ_REHBERI.md](MONTAJ_REHBERI.md)
- [GPS_OPTIMIZASYON_REHBERI.md](GPS_OPTIMIZASYON_REHBERI.md)
- [HC06_BAUD_RATE_AYARLAMA.md](HC06_BAUD_RATE_AYARLAMA.md)
- [mobil-uygulama/MIT_APP_INVENTOR_REHBER.md](mobil-uygulama/MIT_APP_INVENTOR_REHBER.md)
- [android-app/README.md](android-app/README.md)
- [PROJE_DURUM_RAPORU.md](PROJE_DURUM_RAPORU.md)
- [README.md](README.md)
