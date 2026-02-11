# MONTAJ REHBERİ
## GPS Hız ve Mesafe Ölçüm Sistemi

---

## 1. GEREKLİ MALZEMELER

### Elektronik Bileşenler
- [ ] Arduino Uno veya Nano (1 adet) - **Klon/Clone versiyonlar da kullanılabilir**
- [ ] HC-05 veya HC-06 Bluetooth Modülü (1 adet)
- [ ] 16x2 LCD Ekran (I2C modüllü) (1 adet)
- [ ] Jumper Kablolar (Erkek-Erkek, Erkek-Dişi)
- [ ] Breadboard (1 adet)
- [ ] USB Kablosu (Arduino için)
- [ ] 9V Pil + Pil Adaptörü (opsiyonel, mobil kullanım için)

### Voltage Divider için (HC-05 RXD koruması)
- [ ] 1kΩ Direnç (1 adet)
- [ ] 2kΩ Direnç (1 adet)

### Yazılım
- [ ] Arduino IDE (https://www.arduino.cc/en/software)
- [ ] MIT App Inventor Hesabı (https://appinventor.mit.edu)
- [ ] I2C Scanner Sketch (LCD adresini bulmak için)

---

## 2. ARDUINO - BLUETOOTH BAĞLANTISI

### HC-05/HC-06 Pinout

```
HC-05 Modülü:
┌─────────────┐
│    HC-05    │
├─────────────┤
│ VCC  → 5V   │  Kırmızı kablo
│ GND  → GND  │  Siyah kablo
│ TXD  → RX   │  Yeşil kablo (Arduino Pin 10)
│ RXD  → TX   │  Mavi kablo (Arduino Pin 11) + Voltage Divider
│ STATE       │  (Kullanılmıyor)
│ EN          │  (Kullanılmıyor)
└─────────────┘
```

### Bağlantı Şeması

#### DOĞRUDAN BAĞLANTI (Risk: HC-05 RXD zarar görebilir)
```
Arduino          HC-05
 5V     ────────  VCC
 GND    ────────  GND
 Pin 10 ────────  TXD
 Pin 11 ────────  RXD
```

#### GÜVENLİ BAĞLANTI (Voltage Divider ile)
```
Arduino Pin 11 ───┬─── 1kΩ ───┬─── HC-05 RXD
                  │            │
                 GND        2kΩ
                            │
                           GND
```

**Açıklama:**
- Arduino TX (Pin 11) → 5V çıkış verir
- HC-05 RXD → 3.3V toleranslıdır
- Voltage divider: $V_{out} = V_{in} \times \frac{R_2}{R_1 + R_2} = 5V \times \frac{2k\Omega}{3k\Omega} = 3.3V$

---

## 3. ARDUINO - LCD BAĞLANTISI

### I2C LCD Pinout

```
LCD I2C Modülü:
┌─────────────┐
│  16x2 LCD   │
├─────────────┤
│ VCC → 5V    │  Kırmızı kablo
│ GND → GND   │  Siyah kablo
│ SDA → A4    │  Sarı kablo (Arduino Uno/Nano)
│ SCL → A5    │  Turuncu kablo (Arduino Uno/Nano)
└─────────────┘
```

**Not:** Arduino Mega için SDA=20, SCL=21 pinleri kullanılır.

### Bağlantı Şeması

```
Arduino          LCD I2C
 5V     ────────  VCC
 GND    ────────  GND
 A4     ────────  SDA
 A5     ────────  SCL
```

---

## 4. BREADBOARD MONTAJ ŞEMASİ

```
                    BREADBOARD
    ┌────────────────────────────────────┐
    │                                    │
    │  [Arduino Uno]                     │
    │   ┌──────┐                         │
    │   │ USB  │                         │
    │   └──────┘                         │
    │   5V  GND  A0-A5  D0-D13           │
    │    │   │    │ │     │  │           │
    │    │   │    │ │     │  │           │
    │    │   │    │ │     │  │           │
    │  ──┼───┼────┼─┼─────┼──┼──         │
    │    │   │    │ │     │  │           │
    │    │   │    │ │    10 11           │
    │    │   │    │ │     │  │           │
    │    │   │    │ │     │  │           │
    │  [LCD I2C]  │ │   [HC-05]          │
    │   VCC GND SDA SCL  VCC GND TX RX   │
    │    │   │    │ │     │  │   │  │    │
    │    └───┴────┴─┴─────┴──┴───┴──┘    │
    │                                    │
    └────────────────────────────────────┘
```

### Adım Adım Montaj

1. **Arduino'yu breadboard'a yerleştirin** (veya yanına koyun)
2. **LCD I2C'yi breadboard'a takın**
   - VCC → 5V ray
   - GND → GND ray
   - SDA → Arduino A4
   - SCL → Arduino A5
3. **HC-05'i breadboard'a takın**
   - VCC → 5V ray
   - GND → GND ray
   - TXD → Arduino Pin 10 (SoftwareSerial RX)
   - RXD → Arduino Pin 11 (SoftwareSerial TX) + Voltage divider
4. **5V ve GND raylarını Arduino'ya bağlayın**
5. **Tüm bağlantıları kontrol edin** (kısa devre, ters bağlantı)

---

## 5. YAZILIM KURULUMU

### Adım 1: Arduino IDE Kurulumu

1. Arduino IDE'yi indirin: https://www.arduino.cc/en/software
2. IDE'yi açın
3. **Tools** → **Board** → **Arduino Uno** seçin
4. **Tools** → **Port** → USB portunu seçin
   - **Windows:** COM3, COM4, vb.
   - **Linux (Klon Arduino):** /dev/ttyUSB0 (CH340 çipli klonlar için)
   - **Linux (Orijinal):** /dev/ttyACM0
   - **macOS:** /dev/cu.usbserial-*

### Adım 2: Gerekli Kütüphaneleri Yükleyin

#### 2.1 LiquidCrystal_I2C Kütüphanesi
```
Sketch → Include Library → Manage Libraries
Arama: "LiquidCrystal I2C"
Yükle: "LiquidCrystal_I2C by Frank de Brabander"
```

#### 2.2 I2C Adresini Bulma

LCD'nizin I2C adresini bulmak için şu kodu yükleyin:

```cpp
#include <Wire.h>

void setup() {
  Serial.begin(9600);
  Serial.println("I2C Scanner");
  Wire.begin();
}


void loop() {
  byte error, address;
  int nDevices = 0;
  
  Serial.println("Scanning...");
  for (address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    error = Wire.endTransmission();
    
    if (error == 0) {
      Serial.print("I2C device found at 0x");
      if (address < 16) Serial.print("0");
      Serial.println(address, HEX);
      nDevices++;
    }
  }
  
  if (nDevices == 0)
    Serial.println("No I2C devices found");
  
  delay(5000);
}
```

**Sonuç:** `0x27` veya `0x3F` görürseniz, bunu ana kodda kullanın:
```cpp
LiquidCrystal_I2C lcd(0x27, 16, 2); // veya 0x3F
```

### Adım 3: Ana Kodu Yükleyin

1. `arduino_kod.ino` dosyasını Arduino IDE'de açın
2. LCD I2C adresini kontrol edin (satır 15)
3. **Verify** (✓) butonuna tıklayın (hata kontrolü)
4. **Upload** (→) butonuna tıklayın
5. Upload tamamlanınca LCD'de "GPS SİSTEMİ" yazısı görünmeli

### Adım 4: Serial Monitor ile Test

1. **Tools** → **Serial Monitor** açın
2. Baud rate'i **9600** seçin
3. Bluetooth'tan veri gelince burada görünecektir:
   ```
   Gelen Veri: SPEED:0.00,DIST:0.00
   Ayrıştırıldı → Hız: 0.0 km/h | Mesafe: 0.00 km
   ```

---

## 6. BLUETOOTH EŞLEŞTİRME

### Android Telefonda Eşleştirme

1. **Ayarlar** → **Bluetooth** açın
2. HC-05 civarında olduğundan emin olun
3. **Cihazları Tara** / **Scan**
4. "HC-05" veya "HC-06" cihazını bulun
5. Eşleştir'e tıklayın
6. PIN kodu: **1234** veya **0000** (default)
7. Eşleşti ✓

### HC-05 LED Göstergeleri

| LED Durumu | Anlamı |
|-----------|--------|
| Hızlı yanıp sönme | Eşleştirilmemiş, keşfedilebilir |
| Yavaş yanıp sönme (2 saniyede 1) | Eşleştirilmiş, bağlı değil |
| Çift yanıp sönme | Bağlı ✓ |

---

## 7. MIT APP INVENTOR UYGULAMASI

### Uygulama Oluşturma

1. https://appinventor.mit.edu adresine gidin
2. **Create Apps** → **Start new project**
3. Proje adı: "GPSHizOlcer"

### Bileşenler (Designer)

**Palette → Connectivity:**
- BluetoothClient1

**Palette → Sensors:**
- LocationSensor1
- Clock1

**Palette → User Interface:**
- Button_Baglan
- ListPicker_Cihazlar
- Label_Hiz
- Label_Mesafe
- Label_Durum

### Blocks (Kodlama)

(Detaylı bloklar PROJE_REHBERI.md dosyasında bulunmaktadır)

**Temel Mantık:**
1. ListPicker ile Bluetooth cihaz seç
2. Button_Baglan ile bağlan
3. LocationSensor.LocationChanged → Hız ve mesafe hesapla
4. Clock.Timer (1 saniyede bir) → Bluetooth'a gönder

### APK Oluşturma

1. **Build** → **App (provide QR code for .apk)**
2. QR kodu telefonla tara
3. APK'yı indir ve yükle

---

## 8. TEST ADIMLARI

### Sistem Testi

1. **Donanım Kontrolü:**
   - ✓ Arduino'ya güç ver (USB veya pil)
   - ✓ LCD'de başlangıç mesajı görünmeli
   - ✓ HC-05 LED'i yanıp sönmeli

2. **Bluetooth Bağlantısı:**
   - ✓ Telefonda uygulamayı aç
   - ✓ "HC-05" cihazını seç ve bağlan
   - ✓ HC-05 LED'i düzenli yanıp sönmeye başlamalı
   - ✓ LCD'de "Baglanti Kesildi!" mesajı gitmeli

3. **GPS Testi:**
   - ✓ Açık alana çık (GPS sinyali için)
   - ✓ Telefonda GPS açık olduğundan emin ol
   - ✓ Bekle (GPS fix için ~30 saniye)
   - ✓ Hareket et (yürü veya araçla git)

4. **Veri Görüntüleme:**
   - ✓ LCD'de hız değişmeli
   - ✓ Mesafe artmalı
   - ✓ Arduino Serial Monitor'da veri akışını gör

---Arduino Tanınmıyor (Klon Arduino)

**Belirtiler:**
- Bilgisayar Arduino'yu görmüyor
- Port listesinde görünmüyor

**Çözüm (CH340 Driver):**

**Linux:**
```bash
# CH340 driver kontrolü
lsmod | grep ch341

# Kullanıcı izni ekle
sudo usermod -a -G dialout $USER
# Sistemi yeniden başlat

# Port kontrolü
ls /dev/ttyUSB* /dev/ttyACM*
```

**Windows:**
- CH340 driver indir: http://www.wch.cn/downloads/CH341SER_ZIP.html
- Kurulum yap ve Arduino'yu tak

**Detaylı bilgi:** [KLON_ARDUINO_KURULUM.md](KLON_ARDUINO_KURULUM.md) dosyasına bakın.

### 

## 9. SORUN GİDERME

### LCD Boş Görünüyor

**Olası Nedenler:**
- I2C adresi yanlış → I2C Scanner ile kontrol et
- Kontrast ayarı çok düşük → LCD arkasındaki potansiyometre ile ayarla
- Bağlantı hatası → SDA/SCL pinlerini kontrol et

**Çözüm:**
```cpp
LiquidCrystal_I2C lcd(0x3F, 16, 2); // 0x27 yerine 0x3F dene
```

### Bluetooth Bağlanmıyor

**Olası Nedenler:**
- HC-05 eşleştirilmemiş → Telefonda manuel eşleştir
- Ters bağlantı → TXD-RX, RXD-TX kontrolü
- Güç yetersiz → 5V ve GND kontrolü

**Çözüm:**
- HC-05'i reset et (güç kes-aç)
- AT komutları ile test et (baud rate: 38400):
  ```
  AT → OK
  AT+NAME? → +NAME:HC-05
  AT+PSWD? → +PSWD:1234
  ```

### GPS Sinyali Alınmıyor

**Olası Nedenler:**
- Kapalı alanda → Açık alana çık
- GPS izni verilmemiş → Uygulama ayarlarında izin ver
- LocationSensor TimeInterval çok uzun

**Çözüm:**
- Telefon ayarlarından konum servislerini aç
- Mock GPS uygulaması kullan (test için)

### LCD'de Garip Karakterler

**Olası Nedenler:**
- Baud rate uyuşmazlığı
- Bluetooth veri bozulması

**Çözüm:**
```cpp
bluetooth.begin(9600); // HC-05 default
// Değiştir:
bluetooth.begin(38400);
```

### Mesafe Hesaplanmıyor

**Olası Nedenler:**
- GPS hassasiyeti düşük
- Hareketsiz test

**Çözüm:**
- En az 10m hareket et
- `LocationSensor.DistanceInterval = 0` ayarla

---

## 10. GÜVENLİK UYARILARI

⚠️ **Kısa Devre:** VCC ve GND'yi doğrudan bağlamayın
⚠️ **Voltage:** HC-05 RXD 5V'a hassastır, voltage divider kullanın
⚠️ **Sürüş Güvenliği:** Uygulamayı sürüş sırasında kullanmayın
⚠️ **Güç:** 9V pil kullanırken Arduino Vin pinine bağlayın (5V pinine değil)

---

## 11. ÖLÇÜM RESİMLERİ

### Breadboard Görünümü
```
[Arduino] ──── [Breadboard] ──── [HC-05]
    │                │
    │                │
    └───── [LCD I2C] ┘
```

### LCD Çıktısı
```
┌──────────────────┐
│ Hiz:  45.3 km/h │
│ Mesafe: 2.15 km │
└──────────────────┘
```

---

**Montaj Süresi:** ~30-45 dakika  
**Zorluk Seviyesi:** Orta  
**Tavsiye:** Adımları sırayla takip edin, acele etmeyin!

**Başarılar! 🚀**
