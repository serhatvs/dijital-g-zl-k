# GPS Hız ve Mesafe Ölçüm Sistemi
## Mobil Uygulama + Arduino + Bluetooth Projesi

---

## 1. TELEFON TARAFI: MIT APP INVENTOR

### 1.1 GPS Sensöründen Hız Verisi Alma

MIT App Inventor'da **LocationSensor** bileşeni GPS verilerini sağlar.

**Adımlar:**
1. **Palette** → **Sensors** → **LocationSensor** bileşenini ekleyin
2. **Properties** panelinde:
   - `TimeInterval`: 1000 (her 1 saniyede bir güncelleme)
   - `DistanceInterval`: 0 (sürekli güncelleme)
3. **LocationSensor.LocationChanged** eventi kullanılır

**Blocks Mantığı:**
```
When LocationSensor1.LocationChanged
do:
  set global hizMS to LocationSensor1.CurrentSpeed
  set global latitude to LocationSensor1.Latitude
  set global longitude to LocationSensor1.Longitude
```

**Önemli Not:** `CurrentSpeed` değeri **m/s (metre/saniye)** cinsinden gelir.

---

### 1.2 Hız Dönüşümü: m/s → km/h

**Formül:**
```
Hız (km/h) = Hız (m/s) × 3.6
```

**Blocks Mantığı:**
```
When LocationSensor1.LocationChanged
do:
  set global hizMS to LocationSensor1.CurrentSpeed
  set global hizKMH to (global hizMS × 3.6)
  
  // Virgülden sonra 2 basamak
  set global hizKMH to (round (global hizKMH × 100) / 100)
```

**Örnek:**
- GPS: 13.89 m/s → **50 km/h**

---

### 1.3 Alınan Mesafenin Hesaplanması

Mesafe, iki GPS noktası arasındaki uzaklığın toplanmasıyla bulunur.

**Yöntem 1: Haversine Formülü (Küresel Mesafe)**

MIT App Inventor'da hazır fonksiyon yok, manuel hesaplama gerekir:

```
When LocationSensor1.LocationChanged
do:
  if (global eskiLat ≠ 0) then:
    set global mesafe to LocationSensor1.DistanceTo(
      global eskiLat,
      global eskiLon,
      LocationSensor1.Latitude,
      LocationSensor1.Longitude
    )
    // metre → km
    set global toplamMesafe to (global toplamMesafe + global mesafe / 1000)
  
  set global eskiLat to LocationSensor1.Latitude
  set global eskiLon to LocationSensor1.Longitude
```

**Yöntem 2: Basitleştirilmiş Hesaplama**

Kısa mesafeler için yaklaşık hesap:
```
Mesafe (km) = Hız (km/h) × Süre (saat)
```

**Blocks Mantığı:**
```
// Her 1 saniyede
set global mesafeArtis to (global hizKMH / 3600)
set global toplamMesafe to (global toplamMesafe + global mesafeArtis)
```

---

### 1.4 Bluetooth ile Veri Gönderme

**Bileşen:**
- **Palette** → **Connectivity** → **BluetoothClient**

**Adımlar:**

1. **Bağlantı Kurma:**
```
When Button_Baglan.Click
do:
  call BluetoothClient1.ConnectWithUUID(
    ListPicker.Selection,
    "00001101-0000-1000-8000-00805F9B34FB"
  )
```

2. **Veri Gönderme (Clock ile periyodik):**
```
// Clock bileşeni: TimerInterval = 1000 ms
When Clock1.Timer
do:
  if (BluetoothClient1.IsConnected) then:
    set global veriMetni to join(
      "SPEED:",
      global hizKMH,
      ",DIST:",
      global toplamMesafe,
      "\n"
    )
    call BluetoothClient1.SendText(global veriMetni)
```

---

## 2. BLUETOOTH VERİ FORMATI

### Önerilen Format:

```
SPEED:45.50,DIST:1.32\n
```

**Açıklama:**
- `SPEED:` → Hız etiketi
- `45.50` → Hız değeri (km/h)
- `,DIST:` → Mesafe etiketi
- `1.32` → Mesafe değeri (km)
- `\n` → Satır sonu (Arduino'da okuma kolaylığı)

**Alternatif Format (JSON tarzı):**
```
{"speed":45.5,"dist":1.32}\n
```

**Basit Format (sadece değerler):**
```
45.5;1.32\n
```

**Seçim Kriteri:**
Etiketli format (`SPEED:`, `DIST:`) daha okunabilir ve hata ayıklama açısından tercih edilir.

---

## 3. ARDUINO TARAFI

### 3.1 Gerekli Donanım

- Arduino Uno/Nano
- HC-05 veya HC-06 Bluetooth Modülü
- 16x2 LCD Ekran (I2C veya paralel)
- Bağlantı kabloları

### 3.2 Bluetooth Bağlantısı

**HC-05/HC-06 Pinleri:**
```
HC-05 VCC  → Arduino 5V
HC-05 GND  → Arduino GND
HC-05 TXD  → Arduino RX (Pin 0) veya SoftwareSerial (Pin 10)
HC-05 RXD  → Arduino TX (Pin 1) veya SoftwareSerial (Pin 11)
```

**Not:** RXD için 3.3V'a voltage divider gerekebilir.

### 3.3 LCD Bağlantısı

**I2C LCD (LiquidCrystal_I2C kütüphanesi):**
```
LCD SDA → Arduino A4
LCD SCL → Arduino A5
LCD VCC → Arduino 5V
LCD GND → Arduino GND
```

### 3.4 Arduino Kodu

```cpp
#include <SoftwareSerial.h>
#include <LiquidCrystal_I2C.h>

// Bluetooth SoftwareSerial (RX, TX)
SoftwareSerial bluetooth(10, 11);

// LCD I2C adresi (genellikle 0x27 veya 0x3F)
LiquidCrystal_I2C lcd(0x27, 16, 2);

String gelenVeri = "";
float hiz = 0.0;
float mesafe = 0.0;

void setup() {
  Serial.begin(9600);      // Debug için
  bluetooth.begin(9600);   // HC-05 default baud rate
  
  lcd.init();
  lcd.backlight();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("GPS Sistemi");
  lcd.setCursor(0, 1);
  lcd.print("Baslatiliyor...");
  delay(2000);
  lcd.clear();
}

void loop() {
  // Bluetooth'tan veri okuma
  while (bluetooth.available()) {
    char karakter = bluetooth.read();
    
    if (karakter == '\n') {
      // Veri tamamlandı, ayrıştır
      veriAyristir(gelenVeri);
      gelenVeri = "";  // Temizle
    } else {
      gelenVeri += karakter;
    }
  }
  
  // LCD'ye yazdır
  lcdGuncelle();
  delay(100);
}

void veriAyristir(String veri) {
  // Format: SPEED:45.50,DIST:1.32
  
  int speedIndex = veri.indexOf("SPEED:");
  int distIndex = veri.indexOf(",DIST:");
  
  if (speedIndex != -1 && distIndex != -1) {
    // Hız değerini al
    String hizStr = veri.substring(speedIndex + 6, distIndex);
    hiz = hizStr.toFloat();
    
    // Mesafe değerini al
    String mesafeStr = veri.substring(distIndex + 6);
    mesafe = mesafeStr.toFloat();
    
    // Debug
    Serial.print("Hiz: ");
    Serial.print(hiz);
    Serial.print(" km/h | Mesafe: ");
    Serial.print(mesafe);
    Serial.println(" km");
  }
}

void lcdGuncelle() {
  // İlk satır: Hız
  lcd.setCursor(0, 0);
  lcd.print("Hiz: ");
  lcd.print(hiz, 1);  // 1 ondalık basamak
  lcd.print(" km/h  ");
  
  // İkinci satır: Mesafe
  lcd.setCursor(0, 1);
  lcd.print("Mesafe: ");
  lcd.print(mesafe, 2);  // 2 ondalık basamak
  lcd.print(" km ");
}
```

### 3.5 Kod Açıklaması

**Veri Okuma:**
- `bluetooth.available()` → Gelen veri var mı kontrol eder
- `bluetooth.read()` → Karakterleri tek tek okur
- `\n` görünce veri tamamlanmış sayılır

**Veri Ayrıştırma:**
- `indexOf()` → "SPEED:", ",DIST:" etiketlerini bulur
- `substring()` → Değerleri ayırır
- `toFloat()` → String'i sayıya çevirir

**LCD Güncelleme:**
- `lcd.setCursor(sütun, satır)` → İmleç konumu
- `lcd.print()` → Metni yazar
- Ekstra boşluklar eski karakterleri siler

---

## 4. RAPOR İÇİN AKADEMİK ÖZET

### 4.1 Projenin Amacı ve Kapsamı

Bu çalışmada, akıllı telefon GPS sensörü ile elde edilen hız ve konum verilerinin Bluetooth iletişimi üzerinden Arduino mikrodenetleyicisine aktarılması ve LCD ekranda görselleştirilmesi amaçlanmıştır. Proje, mobil uygulama geliştirme, kablosuz iletişim protokolleri ve gömülü sistem entegrasyonunu kapsayan multidisipliner bir yapıya sahiptir.

### 4.2 Sistem Mimarisi

Sistem üç ana bileşenden oluşmaktadır:

**1. Mobil Uygulama Katmanı**
- Platform: MIT App Inventor (visual programming)
- Sensör: GPS/LocationSensor
- İletişim: Bluetooth Client (SPP protokolü)
- Veri İşleme: Hız dönüşümü (m/s → km/h), mesafe hesaplama

**2. İletişim Katmanı**
- Protokol: Bluetooth Serial Port Profile (SPP)
- Modül: HC-05/HC-06 (UART @ 9600 baud)
- Veri Formatı: ASCII metin ("SPEED:X,DIST:Y\n")
- Güncelleme Frekansı: 1 Hz

**3. Gömülü Sistem Katmanı**
- Mikrodenetleyici: Arduino (ATmega328P)
- Arayüz: 16x2 LCD (I2C iletişim)
- Veri İşleme: String parsing, float dönüşüm

### 4.3 Çalışma Prensibi

**Adım 1: Veri Elde Etme**
Mobil uygulama, GPS sensöründen `LocationSensor.CurrentSpeed` parametresi ile anlık hız verisini metre/saniye (m/s) cinsinden alır. Coğrafi konum değişimi (Latitude/Longitude) mesafe hesaplamasında kullanılır.

**Adım 2: Veri Dönüşümü ve Hesaplama**
- Hız dönüşümü: $v_{km/h} = v_{m/s} \times 3.6$
- Mesafe hesaplama: İki ardışık GPS koordinatı arasındaki Haversine mesafesi toplanarak kümülatif mesafe bulunur:

$$
d = 2r \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right)
$$

Burada $r$ Dünya yarıçapı, $\phi$ enlem, $\lambda$ boylam değerleridir.

**Adım 3: Veri İletimi**
Hesaplanan hız ve mesafe değerleri, belirlenmiş formatta (örn. "SPEED:45.5,DIST:1.32\n") Bluetooth üzerinden seri iletişim ile gönderilir. Her veri paketi newline karakteri (\n) ile sonlandırılarak Arduino'nun paket sınırlarını tanıması sağlanır.

**Adım 4: Veri Alımı ve Ayrıştırma**
Arduino, SoftwareSerial kütüphanesi ile Bluetooth modülünden gelen karakterleri buffer'da toplar. Newline karakteri algılandığında, `indexOf()` ve `substring()` fonksiyonları ile etiketler ("SPEED:", "DIST:") bulunur ve sayısal değerler `toFloat()` ile parse edilir.

**Adım 5: Görselleştirme**
Ayrıştırılan değerler, LiquidCrystal_I2C kütüphanesi kullanılarak 16x2 LCD ekranın birinci satırında "Hız: X km/h", ikinci satırında "Mesafe: Y km" formatında gösterilir.

### 4.4 Teknik Zorluklar ve Çözümler

| Zorluk | Çözüm |
|--------|-------|
| GPS sinyalinin kapalı alanlarda kesilmesi | Açık alanda test, mock veri kullanımı |
| Bluetooth bağlantı kopmaları | Bağlantı durumu kontrolü, otomatik yeniden bağlanma |
| LCD üzerinde karakter artıkları | Ekstra boşluk karakterleri ile eski veriyi silme |
| HC-05 RXD 5V toleransı | Voltage divider (1kΩ + 2kΩ direnç) |

### 4.5 Test ve Doğrulama

**Test Senaryoları:**
1. Sabit konum: GPS verisi 0 km/h göstermeli
2. Yürüyüş (4-6 km/h): Hız ve mesafe tutarlı artmalı
3. Araç testi (30-50 km/h): Gerçek zamanlı güncelleme
4. Bluetooth menzil testi: ~10m mesafede stabil iletişim

**Doğrulama:**
- Gerçek GPS hızı ile LCD'deki değer karşılaştırması
- Bilinen mesafede ölçüm (örn. 1 km yol)
- Serial Monitor ile Arduino gelen veri kontrolü

### 4.6 Sonuç ve Öneriler

Proje başarıyla tamamlanmış, GPS verilerinin Bluetooth üzerinden Arduino'ya aktarımı ve LCD ekranda gösterimi gerçekleştirilmiştir. Sistem, düşük maliyetli bileşenler kullanarak gerçek zamanlı veri iletimi sağlamaktadır.

**Gelecek Geliştirmeler:**
- SD kart ile veri kaydetme (data logging)
- Web sunucu entegrasyonu (ESP32 ile)
- Hız limiti uyarı sistemi
- Batarya optimizasyonu (GPS TimeInterval ayarı)

---

## 5. KAYNAK KOD VE ÖRNEK BLOKLAR

### 5.1 MIT App Inventor Blocks (Pseudo-code)

```
Global Variables:
  - hizMS (number) = 0
  - hizKMH (number) = 0
  - toplamMesafe (number) = 0
  - eskiLat (number) = 0
  - eskiLon (number) = 0

When Screen1.Initialize:
  set Clock1.TimerInterval to 1000
  set LocationSensor1.TimeInterval to 1000

When LocationSensor1.LocationChanged:
  // Hız hesaplama
  set global hizMS to LocationSensor1.CurrentSpeed
  set global hizKMH to round((hizMS × 3.6) × 100) / 100
  
  // Mesafe hesaplama
  if (global eskiLat ≠ 0):
    set mesafeM to LocationSensor1.DistanceTo(
      global eskiLat,
      global eskiLon,
      LocationSensor1.Latitude,
      LocationSensor1.Longitude
    )
    set global toplamMesafe to toplamMesafe + (mesafeM / 1000)
  
  set global eskiLat to LocationSensor1.Latitude
  set global eskiLon to LocationSensor1.Longitude

When Clock1.Timer:
  if BluetoothClient1.IsConnected:
    set veriMetni to join(
      "SPEED:", global hizKMH,
      ",DIST:", round(global toplamMesafe × 100) / 100,
      "\n"
    )
    call BluetoothClient1.SendText(veriMetni)
    set Label_Hiz.Text to join("Hız: ", global hizKMH, " km/h")
    set Label_Mesafe.Text to join("Mesafe: ", global toplamMesafe, " km")
```

### 5.2 Arduino - Alternatif Ayrıştırma Yöntemi

```cpp
void veriAyristir2(String veri) {
  // Format: SPEED:45.50,DIST:1.32
  veri.replace("SPEED:", "");
  veri.replace("DIST:", "");
  
  int virguIndex = veri.indexOf(',');
  
  if (virguIndex != -1) {
    hiz = veri.substring(0, virguIndex).toFloat();
    mesafe = veri.substring(virguIndex + 1).toFloat();
  }
}
```

---

## 6. SORU VE CEVAPLAR

**S1: GPS hassasiyeti nedir?**
**C:** Tüketici GPS'leri ~5-10m hassasiyete sahiptir. Hız ölçümü daha doğrudur (±0.5 km/h).

**S2: Bluetooth menzili yeterli mi?**
**C:** HC-05 ~10m menzile sahiptir. Araç içi kullanım için yeterlidir.

**S3: MIT App Inventor yerine native Android kullanılmalı mı?**
**C:** Prototip için MIT yeterlidir. Production için Java/Kotlin önerilir.

**S4: LCD yerine OLED kullanılabilir mi?**
**C:** Evet, SSD1306 OLED ekran kullanılabilir (I2C aynı).

**S5: Veri kaybı olursa ne olur?**
**C:** Bluetooth buffer dolabilir. Flow control veya daha uzun interval gerekir.

---

## 7. KAYNAKLAR

1. MIT App Inventor Documentation: http://ai2.appinventor.mit.edu
2. Arduino Bluetooth Communication: https://www.arduino.cc/en/Reference/SoftwareSerial
3. GPS Haversine Formula: https://en.wikipedia.org/wiki/Haversine_formula
4. HC-05 Datasheet: https://components101.com/wireless/hc-05-bluetooth-module
5. LiquidCrystal_I2C Library: https://github.com/johnrickman/LiquidCrystal_I2C

---

**Proje Hazırlayan:** Öğrenci Projesi  
**Tarih:** Şubat 2026  
**Versiyon:** 1.0
