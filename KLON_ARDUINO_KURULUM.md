# ARDUINO UNO KLON KURULUM REHBERİ

## Arduino Uno Klon Nedir?

Arduino Uno **klon** (clone) kartları, orijinal Arduino Uno'nun daha ucuz versiyonlarıdır. Genellikle Çin üretimi olup, aynı şematiği kullanır ancak bazı bileşenler farklıdır.

### Orijinal vs Klon Farkları

| Özellik | Orijinal Arduino Uno | Arduino Uno Klon |
|---------|---------------------|------------------|
| USB-Serial Çip | ATmega16U2 | **CH340G** veya CH341 |
| Fiyat | $25-30 | $3-5 |
| Kalite | Yüksek | Değişken |
| Driver | Otomatik tanınır | Manuel kurulum gerekebilir |

---

## 1. CH340/CH341 DRIVER KURULUMU

Klon Arduino'lar **CH340G** USB-Serial çipi kullanır. Linux'ta genellikle otomatik çalışır, ancak bazı sistemlerde manuel kurulum gerekir.

### Linux (Ubuntu/Debian)

#### Adım 1: Driver Kontrolü

```bash
# CH340 modülünün yüklü olup olmadığını kontrol et
lsmod | grep ch341

# Veya
dmesg | grep ch34
```

#### Adım 2: Arduino'yu Bağla ve Test Et

```bash
# USB cihazlarını listele
lsusb

# CH340 görünmeli:
# Bus 001 Device 005: ID 1a86:7523 QinHeng Electronics CH340 serial converter

# Seri portları kontrol et
ls -la /dev/ttyUSB*

# Çıktı: /dev/ttyUSB0 (klon için)
# Orijinal Arduino: /dev/ttyACM0
```

#### Adım 3: Driver Kurulumu (Gerekirse)

```bash
# CH340 driver'ı genellikle kernel'da vardır
# Yoksa:
sudo apt-get update
sudo apt-get install linux-headers-$(uname -r)

# Modülü yükle
sudo modprobe ch341
```

#### Adım 4: Kullanıcı İzinleri

```bash
# Dialout grubuna kullanıcıyı ekle (seri port erişimi için)
sudo usermod -a -G dialout $USER

# Sistemi yeniden başlat veya:
newgrp dialout

# İzinleri kontrol et
groups | grep dialout
```

#### Adım 5: Port İzinlerini Test Et

```bash
# Arduino bağlıyken:
ls -l /dev/ttyUSB0

# Çıktı: crw-rw---- 1 root dialout ...
# dialout grubu erişim hakkına sahip olmalı
```

### Windows

1. CH340 driver'ı indir: http://www.wch.cn/downloads/CH341SER_ZIP.html
2. .zip dosyasını çıkart
3. `CH341SER.EXE` çalıştır
4. "INSTALL" butonuna tıkla
5. Arduino'yu tak → Device Manager'da "USB-SERIAL CH340 (COM3)" görünmeli

### macOS

```bash
# Homebrew ile kur
brew tap adrianmihalko/ch340g-ch34g-ch34x-mac-os-x-driver
brew cask install wch-ch34x-usb-serial-driver

# Veya manuel:
# https://github.com/adrianmihalko/ch340g-ch34g-ch34x-mac-os-x-driver/
```

---

## 2. ARDUINO CLI İLE KULLANIM

### Port Tespiti

```bash
# Bağlı Arduino'ları listele
./bin/arduino-cli board list

# Çıktı örneği:
# Port         Protocol Type              Board Name FQBN Core
# /dev/ttyUSB0 serial   Serial Port (USB) Unknown
```

**Not:** Klon Arduino genellikle `/dev/ttyUSB0`, orijinal `/dev/ttyACM0` kullanır.

### Kod Derleme

```bash
cd /home/serhat/vscode-workspace/dijital-gozluk

# Arduino Uno için derle
./bin/arduino-cli compile --fqbn arduino:avr:uno arduino_kod.ino

# Çıktı:
# Sketch uses XXX bytes (X%) of program storage space.
# Global variables use XXX bytes (X%) of dynamic memory.
```

### Kod Yükleme

```bash
# Arduino Uno klona yükle
./bin/arduino-cli upload -p /dev/ttyUSB0 --fqbn arduino:avr:uno arduino_kod.ino

# Veya port otomatik tespit:
./bin/arduino-cli upload --fqbn arduino:avr:uno arduino_kod.ino

# Upload başarılı olursa:
# Used library Version Path
# SoftwareSerial 1.0     /home/.../libraries/SoftwareSerial
# LiquidCrystal I2C 1.1.2  /home/.../libraries/LiquidCrystal_I2C
```

### Serial Monitor

```bash
# Arduino'dan gelen verileri oku
./bin/arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=9600

# Çıkmak için: Ctrl+C
```

---

## 3. KLON ARDUINO İLE SORUN GİDERME

### Problem 1: Port Bulunamıyor

**Hata:**
```
Error during Upload: No upload port provided
```

**Çözüm:**
```bash
# Portu manuel belirt
./bin/arduino-cli upload -p /dev/ttyUSB0 --fqbn arduino:avr:uno arduino_kod.ino

# Veya arduino bağlıyken:
ls /dev/ttyUSB* /dev/ttyACM*
```

### Problem 2: Permission Denied

**Hata:**
```
avrdude: ser_open(): can't open device "/dev/ttyUSB0": Permission denied
```

**Çözüm:**
```bash
# Kullanıcıyı dialout grubuna ekle
sudo usermod -a -G dialout $USER

# Çıkış yap ve tekrar giriş yap
# VEYA sistemi yeniden başlat
```

**Geçici Çözüm:**
```bash
# Sadece bu oturum için
sudo chmod 666 /dev/ttyUSB0

# Veya
sudo ./bin/arduino-cli upload -p /dev/ttyUSB0 --fqbn arduino:avr:uno arduino_kod.ino
```

### Problem 3: Upload Timeout

**Hata:**
```
avrdude: stk500_recv(): programmer is not responding
```

**Çözüm:**
1. Arduino'nun USB kablosunu çıkar-tak
2. Arduino'daki RESET butonuna bas
3. Upload sırasında TX/RX ledleri yanıp sönmeli
4. Bluetooth modülünü geçici olarak çıkar (RX/TX pinleri çakışabilir)

**Kritik:** Upload sırasında Arduino'nun RX (Pin 0) ve TX (Pin 1) pinleri boş olmalı!

**Çözüm Kodu:**
```cpp
// SoftwareSerial kullanarak çakışmayı önle
// arduino_kod.ino zaten SoftwareSerial kullanıyor (Pin 10, 11)
// Bu yüzden upload sırasında sorun olmamalı
```

### Problem 4: CH340 Driver Çalışmıyor

**Belirtiler:**
- `lsusb` çıktısında CH340 görünüyor ama `/dev/ttyUSB0` yok
- `dmesg | tail` çıktısında hata var

**Çözüm:**
```bash
# Kernel loglarını kontrol et
dmesg | tail -20

# CH340 modülünü yeniden yükle
sudo rmmod ch341
sudo modprobe ch341

# Veya sistem güncellemesi
sudo apt-get update
sudo apt-get upgrade linux-image-generic
```

### Problem 5: Yanlış Board Seçimi

**Hata:**
```
avrdude: Device signature = 0x1e950f (probably m328p)
avrdude: Expected signature for ATmega328 is 1E 95 14
```

**Çözüm:**
```bash
# Arduino Uno için doğru FQBN:
--fqbn arduino:avr:uno

# Arduino Nano için:
--fqbn arduino:avr:nano:cpu=atmega328

# Diğer board'ları listele:
./bin/arduino-cli board listall
```

---

## 4. KLON ARDUİNO DONANIM KONTROL

### Pin Testi

Arduino'nun doğru çalıştığını test etmek için basit bir Blink kodu:

```bash
# Test dosyası oluştur
cat > blink_test.ino << 'EOF'
void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  digitalWrite(LED_BUILTIN, HIGH);
  delay(1000);
  digitalWrite(LED_BUILTIN, LOW);
  delay(1000);
}
EOF

# Derle ve yükle
./bin/arduino-cli compile --fqbn arduino:avr:uno blink_test.ino
./bin/arduino-cli upload -p /dev/ttyUSB0 --fqbn arduino:avr:uno blink_test.ino

# Arduino'daki LED (Pin 13) yanıp sönmeli
```

### Voltage Testi

```cpp
// Analog pin testi
void setup() {
  Serial.begin(9600);
}

void loop() {
  int val = analogRead(A0);
  Serial.println(val);
  delay(500);
}

// A0 pinine 3.3V bağla → ~675 okur
// A0 pinine 5V bağla → ~1023 okur
```

---

## 5. PROJE İÇİN ÖNEMLİ NOTLAR

### HC-06 ve Upload Sorunu

**Problem:** Arduino'ya kod yüklerken HC-06 RX/TX pinlerine bağlıysa çakışma olur.

**Çözüm 1:** SoftwareSerial kullan (bizim projede zaten var)
```cpp
SoftwareSerial bluetooth(10, 11); // RX, TX
// Pin 0 ve Pin 1 boş kalır
```

**Çözüm 2:** Upload sırasında HC-05'i çıkar
```
1. HC-05 TXD ve RXD kablolarını çıkar
2. Arduino'ya kod yükle
3. Kablolarını geri tak
```

### Güç Tüketimi

Klon Arduino'lar orijinalden daha fazla güç çekebilir:

```
Orijinal Arduino Uno: ~50mA (idle)
Klon Arduino Uno: ~50-80mA (idle)
HC-06 Bluetooth: ~50mA (bağlı)
LCD 16x2: ~20mA
Toplam: ~150-200mA

Önerilen: 5V 1A adaptör veya USB power bank
```

### Klon Arduino Kalite Kontrol

```bash
# Voltaj testi
./bin/arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=9600

# Arduino'da:
void setup() {
  Serial.begin(9600);
  Serial.print("5V Pin: ");
  Serial.print(analogRead(A0) * (5.0 / 1023.0));
  Serial.println(" V");
}

# 5V pin 4.8V-5.2V arası olmalı
# Daha düşükse güç kaynağı yetersiz
```

---

## 6. ARDUINO IDE KULLANIMI (Opsiyonel)

Arduino CLI yerine Arduino IDE kullanmak isterseniz:

### Kurulum

```bash
# Linux için
sudo snap install arduino

# Veya manuel:
# https://www.arduino.cc/en/software
```

### Ayarlar

1. **Tools** → **Board** → **Arduino Uno**
2. **Tools** → **Port** → **/dev/ttyUSB0** (klon için)
3. **Tools** → **Processor** → **ATmega328P**
4. **Sketch** → **Verify/Compile**
5. **Sketch** → **Upload**

### Kütüphane Kurulumu

1. **Sketch** → **Include Library** → **Manage Libraries**
2. Ara: "LiquidCrystal I2C"
3. Yükle: "LiquidCrystal I2C by Frank de Brabander"

---

## 7. HİZLI BAŞLANGIÇ KOMUTU

Tüm kurulumu tek komutta yap:

```bash
#!/bin/bash
# klon_arduino_kurulum.sh

echo "=== Arduino Uno Klon Kurulum ==="

# CH340 driver kontrol
if lsmod | grep -q ch341; then
    echo "✓ CH340 driver yüklü"
else
    echo "✗ CH340 driver yükleniyor..."
    sudo modprobe ch341
fi

# Kullanıcı izinleri
if groups | grep -q dialout; then
    echo "✓ Dialout izni var"
else
    echo "✗ Dialout izni ekleniyor..."
    sudo usermod -a -G dialout $USER
    echo "! Oturumu kapat-aç veya sistemi yeniden başlat"
fi

# Port tespit
PORT=$(ls /dev/ttyUSB* /dev/ttyACM* 2>/dev/null | head -n1)
if [ -z "$PORT" ]; then
    echo "✗ Arduino bulunamadı! Bağlantıyı kontrol et"
else
    echo "✓ Arduino bulundu: $PORT"
fi

# Blink testi
echo "Blink testi yapılıyor..."
cat > /tmp/blink.ino << 'EOF'
void setup() { pinMode(LED_BUILTIN, OUTPUT); }
void loop() { 
  digitalWrite(LED_BUILTIN, HIGH); delay(1000);
  digitalWrite(LED_BUILTIN, LOW); delay(1000);
}
EOF

./bin/arduino-cli compile --fqbn arduino:avr:uno /tmp/blink.ino
./bin/arduino-cli upload -p $PORT --fqbn arduino:avr:uno /tmp/blink.ino

echo "=== Kurulum Tamamlandı ==="
```

Kullanım:
```bash
chmod +x klon_arduino_kurulum.sh
./klon_arduino_kurulum.sh
```

---

## 8. KAYNAK VE REFERANSLAR

- CH340 Linux Driver: https://github.com/juliagoda/CH341SER
- Arduino CLI Docs: https://arduino.github.io/arduino-cli/
- Arduino Uno Pinout: https://docs.arduino.cc/hardware/uno-rev3
- Klon Arduino Forum: https://forum.arduino.cc/t/chinese-clone-arduino/

---

**Özet:**
- ✅ Klon Arduino CH340 çipi kullanır
- ✅ Linux'ta genellikle `/dev/ttyUSB0` olarak görünür
- ✅ `dialout` grubuna kullanıcı eklenmeli
- ✅ Upload sırasında RX/TX pinleri boş olmalı
- ✅ SoftwareSerial kullanarak çakışma önlenir

**Başarılar! 🚀**
