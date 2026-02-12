# HC-06 Baud Rate Ayarlama Rehberi

## Mevcut Durum
- Şu an: **9600 baud** (varsayılan)
- Hedef: **115200 baud** (12 kat daha hızlı)

## Neden Yüksek Baud Rate?
- 9600 baud: ~17 mesaj/saniye
- 115200 baud: ~209 mesaj/saniye
- GPS smoothing için saniyede 10 veri topluyoruz

## Adımlar

### 1. HC-06'yı Arduino'ya Bağlayın
Mevcut bağlantınız zaten doğru.

### 2. AT Komut Modu Kodu Yükleyin

`HC06_AT_Commander.ino` adında yeni dosya:
```cpp
#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11); // RX, TX

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600); // HC-06 başlangıç baud rate
  
  Serial.println("=== HC-06 AT Commander ===");
  Serial.println("AT komutlari girin:");
  Serial.println("AT -> Test");
  Serial.println("AT+BAUD8 -> 115200 baud");
  Serial.println("AT+NAMEKayakCam -> Isim degistir");
}

void loop() {
  // Serial -> Bluetooth
  if (Serial.available()) {
    char c = Serial.read();
    bluetooth.write(c);
  }
  
  // Bluetooth -> Serial
  if (bluetooth.available()) {
    char c = bluetooth.read();
    Serial.write(c);
  }
}
```

### 3. Kodu Yükleyin ve Serial Monitor Açın
```bash
cd ~/vscode-workspace/dijital-gozluk
./bin/arduino-cli compile --fqbn arduino:avr:uno HC06_AT_Commander/
sudo chmod 666 /dev/ttyUSB0
./bin/arduino-cli upload -p /dev/ttyUSB0 --fqbn arduino:avr:uno HC06_AT_Commander/
screen /dev/ttyUSB0 9600
```

### 4. AT Komutlarını Gönderin

**DİKKAT:** HC-06 bağlantı kurulmamışken (LED yanıp sönüyorken) AT komutlarına cevap verir.

```
AT           -> Yanıt: OK (test)
AT+VERSION   -> Versiyonu göster
AT+BAUD8     -> 115200 baud ayarla (BAUD4=9600, BAUD8=115200)
AT+NAMEKayakCam -> İsmi değiştir
```

### 5. Baud Rate Kodları
- BAUD1: 1200
- BAUD2: 2400
- BAUD3: 4800
- BAUD4: 9600 (varsayılan)
- BAUD5: 19200
- BAUD6: 38400
- BAUD7: 57600
- BAUD8: 115200

### 6. Ana Kodu Güncelleyin

`arduino_kod.ino`:
```cpp
void setup() {
  Serial.begin(115200);  // ← Değişti
  bluetooth.begin(115200); // ← Değişti (HC-06 şimdi 115200'de)
  
  // ... geri kalan kod aynı
}
```

## Test Etme

1. AT komutuyla ayarlayın
2. Ana kodu 115200 ile güncelleyin
3. Serial monitörü 115200'de açın
4. GPS verilerinin akmasını izleyin

## Geri Alma

Eğer sorun olursa:
```
AT+BAUD4  -> 9600 baud'a geri dön
```

## Performans

**Öncesi (9600 baud):**
- Veri güncelleme: 1 saniye/mesaj
- Transfer hızı: ~960 byte/s

**Sonrası (115200 baud):**
- Veri güncelleme: 0.1 saniye/mesaj (10 Hz)
- Transfer hızı: ~11,520 byte/s
- GPS smoothing: 10 örnek ortalaması (1 saniye)

## Sorun Giderme

**Soru:** AT komutlarına cevap vermiyor?
**Cevap:** HC-06 LED'i yanıp sönmeli (unpaired mod). Telefonu disconnect edin.

**Soru:** Yanlış baud rate ayarlandı, iletişim kesildi?
**Cevap:** Tüm baud rate'leri deneyin (1200-115200 arası). Arduino kodunu değiştirip test edin.

**Soru:** 115200'de veri bozuk geliyor?
**Cevap:** Kablo kalitesi önemli. 57600'e düşürün (AT+BAUD7) - yeterince hızlıdır.
