/*
 * HC-06 AT KOMUT MODU
 * 
 * Bu kod ile HC-06 Bluetooth modülünün ayarlarını değiştirebilirsiniz:
 * - Baud rate (hız)
 * - İsim
 * - PIN kod
 * 
 * KULLANIM:
 * 1. Bu kodu Arduino'ya yükleyin
 * 2. Serial Monitor'u açın (9600 baud)
 * 3. HC-06'nın LED'i yanıp sönsün (unpaired - bağlantısız)
 * 4. AT komutlarını gönderin
 * 
 * ÖNEMLİ KOMUTLAR:
 * AT           -> Test (Yanıt: OK)
 * AT+VERSION   -> Versiyon bilgisi
 * AT+BAUD8     -> 115200 baud ayarla
 * AT+NAMEKayakCam -> İsim değiştir
 * AT+PIN1234   -> PIN değiştir
 */

#include <SoftwareSerial.h>

#define BT_RX 10  // Arduino RX -> HC-06 TX
#define BT_TX 11  // Arduino TX -> HC-06 RX

SoftwareSerial bluetooth(BT_RX, BT_TX);

void setup() {
  // Bilgisayar ile iletişim
  Serial.begin(9600);
  Serial.println("==============================");
  Serial.println("   HC-06 AT COMMANDER");
  Serial.println("==============================");
  Serial.println();
  Serial.println("HC-06 LED yanip sondugunden emin olun!");
  Serial.println("(Bluetooth baglantisi olmamali)");
  Serial.println();
  Serial.println("--- KULLANILABILIR KOMUTLAR ---");
  Serial.println("AT             -> Test (OK donmeli)");
  Serial.println("AT+VERSION     -> Versiyon");
  Serial.println("AT+BAUD4       -> 9600 baud (varsayilan)");
  Serial.println("AT+BAUD7       -> 57600 baud (tavsiye)");
  Serial.println("AT+BAUD8       -> 115200 baud (max)");
  Serial.println("AT+NAMEKayakCam -> Isim degistir");
  Serial.println("AT+PIN1234     -> PIN degistir");
  Serial.println();
  Serial.println("Bekleniyor...");
  Serial.println("==============================");
  
  // HC-06 ile iletişim (varsayılan 9600 baud)
  bluetooth.begin(9600);
}

void loop() {
  // Bilgisayardan gelen komutları HC-06'ya gönder
  if (Serial.available()) {
    char c = Serial.read();
    bluetooth.write(c);
    // Echo - ne gönderildiğini göster
    Serial.write(c);
  }
  
  // HC-06'dan gelen yanıtları bilgisayara gönder
  if (bluetooth.available()) {
    char c = bluetooth.read();
    Serial.write(c);
  }
}
