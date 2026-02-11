/*
 * GPS HIZ VE MESAFE ÖLÇÜM SİSTEMİ
 * Bluetooth (HC-05) + LCD (16x2 I2C)
 * 
 * Proje: Telefon GPS verisini Bluetooth ile alıp LCD'de gösterme
 * Tarih: Şubat 2026
 */

#include <SoftwareSerial.h>
#include <LiquidCrystal_I2C.h>

// ==================== PIN TANIMLARI ====================
#define BT_RX 10  // Arduino'nun RX pini → HC-05 TX'e
#define BT_TX 11  // Arduino'nun TX pini → HC-05 RX'e

// ==================== NESNE TANIMLARI ====================
SoftwareSerial bluetooth(BT_RX, BT_TX);
LiquidCrystal_I2C lcd(0x27, 16, 2); // Adres: 0x27 veya 0x3F (I2C scanner ile bulun)

// ==================== GLOBAL DEĞİŞKENLER ====================
String gelenVeri = "";
float hiz = 0.0;
float mesafe = 0.0;
bool bluetoothBagli = false;

// ==================== SETUP ====================
void setup() {
  // Seri port başlat (Debug için)
  Serial.begin(9600);
  Serial.println("=== GPS Hiz ve Mesafe Sistemi ===");
  Serial.println("Bluetooth baglantisi bekleniyor...");
  
  // Bluetooth başlat
  bluetooth.begin(9600); // HC-05 default baud rate
  
  // LCD başlat
  lcd.init();
  lcd.backlight();
  lcd.clear();
  
  // Başlangıç mesajı
  baslatmaMesaji();
  
  delay(2000);
  lcd.clear();
}

// ==================== LOOP ====================
void loop() {
  // 1. Bluetooth'tan veri oku
  bluetoothOku();
  
  // 2. LCD'yi güncelle
  lcdGuncelle();
  
  // 3. Bağlantı durumunu kontrol et
  baglantiKontrol();
  
  delay(100); // CPU yükünü azalt
}

// ==================== FONKSİYONLAR ====================

/**
 * Başlangıç mesajını LCD'de gösterir
 */
void baslatmaMesaji() {
  lcd.setCursor(0, 0);
  lcd.print("  GPS SISTEMI   ");
  lcd.setCursor(0, 1);
  lcd.print(" Baslatiliyor...");
}

/**
 * Bluetooth'tan gelen verileri okur ve işler
 */
void bluetoothOku() {
  while (bluetooth.available()) {
    char karakter = bluetooth.read();
    
    // Newline karakteri gelirse veri tamamlanmış demektir
    if (karakter == '\n' || karakter == '\r') {
      if (gelenVeri.length() > 0) {
        // Debug: Gelen veriyi göster
        Serial.print("Gelen Veri: ");
        Serial.println(gelenVeri);
        
        // Veriyi ayrıştır
        veriAyristir(gelenVeri);
        
        // Buffer'ı temizle
        gelenVeri = "";
        
        bluetoothBagli = true;
      }
    } else {
      // Karakteri buffer'a ekle
      gelenVeri += karakter;
    }
  }
}

/**
 * Gelen veriyi ayrıştırır
 * Format: SPEED:45.50,DIST:1.32
 */
void veriAyristir(String veri) {
  // "SPEED:" ve ",DIST:" etiketlerini bul
  int speedIndex = veri.indexOf("SPEED:");
  int distIndex = veri.indexOf(",DIST:");
  
  if (speedIndex != -1 && distIndex != -1) {
    // Hız değerini al
    String hizStr = veri.substring(speedIndex + 6, distIndex);
    hiz = hizStr.toFloat();
    
    // Mesafe değerini al
    String mesafeStr = veri.substring(distIndex + 6);
    mesafe = mesafeStr.toFloat();
    
    // Debug: Ayrıştırılan değerleri göster
    Serial.print("Ayrıştırıldı → Hız: ");
    Serial.print(hiz, 1);
    Serial.print(" km/h | Mesafe: ");
    Serial.print(mesafe, 2);
    Serial.println(" km");
  } else {
    Serial.println("HATA: Veri formatı hatalı!");
  }
}

/**
 * LCD ekranı günceller
 */
void lcdGuncelle() {
  if (bluetoothBagli) {
    // İlk satır: Hız
    lcd.setCursor(0, 0);
    lcd.print("Hiz:");
    
    // Hız değerini sağa hizalı göster
    if (hiz < 10) {
      lcd.print("  ");
    } else if (hiz < 100) {
      lcd.print(" ");
    }
    lcd.print(hiz, 1);
    lcd.print(" km/h ");
    
    // İkinci satır: Mesafe
    lcd.setCursor(0, 1);
    lcd.print("Mesafe: ");
    lcd.print(mesafe, 2);
    lcd.print(" km  ");
  }
}

/**
 * Bluetooth bağlantı durumunu kontrol eder
 */
void baglantiKontrol() {
  static unsigned long sonGuncelleme = 0;
  unsigned long simdikiZaman = millis();
  
  // 3 saniyedir veri gelmiyorsa bağlantı kesilmiş sayılır
  if (simdikiZaman - sonGuncelleme > 3000 && bluetoothBagli) {
    bluetoothBagli = false;
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Baglanti");
    lcd.setCursor(0, 1);
    lcd.print("Kesildi!");
    Serial.println("UYARI: Bluetooth bağlantısı kesildi!");
  }
  
  if (bluetooth.available()) {
    sonGuncelleme = simdikiZaman;
  }
}

// ==================== ALTERNATİF VERİ AYRIŞTIRMA ====================

/**
 * Alternatif ayrıştırma yöntemi (daha basit format için)
 * Format: 45.5;1.32
 */
void veriAyristir_Basit(String veri) {
  int noktaliVirguIndex = veri.indexOf(';');
  
  if (noktaliVirguIndex != -1) {
    hiz = veri.substring(0, noktaliVirguIndex).toFloat();
    mesafe = veri.substring(noktaliVirguIndex + 1).toFloat();
  }
}

/**
 * JSON formatı için ayrıştırma
 * Format: {"speed":45.5,"dist":1.32}
 * Not: ArduinoJson kütüphanesi gerektirir
 */
/*
#include <ArduinoJson.h>

void veriAyristir_JSON(String veri) {
  StaticJsonDocument<200> doc;
  DeserializationError error = deserializeJson(doc, veri);
  
  if (!error) {
    hiz = doc["speed"];
    mesafe = doc["dist"];
  } else {
    Serial.println("JSON parse hatası!");
  }
}
*/
