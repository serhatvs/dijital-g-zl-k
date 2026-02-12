/*
 * GPS HIZ VE MESAFE ÖLÇÜM SİSTEMİ
 * Bluetooth (HC-06) + LCD (16x2 I2C)
 * 
 * Proje: Telefon GPS verisini Bluetooth ile alıp LCD'de gösterme
 * Tarih: Şubat 2026
 */

#include <SoftwareSerial.h>
#include <LiquidCrystal_I2C.h>

// ==================== PIN TANIMLARI ====================
#define BT_RX 10  // Arduino'nun RX pini → HC-06 TX'e
#define BT_TX 11  // Arduino'nun TX pini → HC-06 RX'e

// ==================== NESNE TANIMLARI ====================
SoftwareSerial bluetooth(BT_RX, BT_TX);
LiquidCrystal_I2C lcd(0x27, 16, 2); // Adres: 0x27 veya 0x3F (I2C scanner ile bulun)

// ==================== GLOBAL DEĞİŞKENLER ====================
String gelenVeri = "";
float hiz = 0.0;
float mesafe = 0.0;
float enlem = 0.0;   // Latitude
float boylam = 0.0;  // Longitude
bool bluetoothBagli = false;
unsigned long sonVeriMillis = 0;
bool testModu = false;  // Test modu aktif mi?
String sonDurum = "";   // Son LCD durumu (gereksiz clear'dan kaçınmak için)

// ==================== SETUP ====================
void setup() {
  // Seri port başlat (Debug için)
  Serial.begin(9600);
  Serial.println("=== GPS Hiz ve Mesafe Sistemi ===");
  Serial.println("Bluetooth baglantisi bekleniyor...");
  Serial.println("Test icin 'TEST' yazin");
  
  // Bluetooth başlat
  bluetooth.begin(9600); // HC-06 default baud rate
  
  // LCD başlat
  lcd.init();
  lcd.backlight();
  lcd.clear();
  
  // Başlangıç mesajı
  baslatmaMesaji();
  
  delay(2000);
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("BT Bekleniyor...");
}

// ==================== LOOP ====================
void loop() {
  // 1. Bluetooth'tan veri oku
  bluetoothOku();
  
  // 2. LCD'yi güncelle
  lcdGuncelle();
  
  // 3. Bağlantı durumunu kontrol et (test modunda değilse)
  if (!testModu) {
    baglantiKontrol();
  }
  
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
        
        // Test senaryosu kontrolü
        if (gelenVeri.equalsIgnoreCase("TEST")) {
          testSenaryosuBaslat();
        } else {
          // Veriyi ayrıştır (Android Locale.US kullandığı için her zaman nokta gelir)
          veriAyristir(gelenVeri);
          
          // Son veri zamanını güncelle
          sonVeriMillis = millis();
          bluetoothBagli = true;
          testModu = false;
          
          // Yeniden bağlandıysa ekranı temizle
          if (sonDurum == "DISCONNECTED") {
            lcd.clear();
            sonDurum = "RECONNECT";
          }
        }
        
        // Buffer'ı temizle
        gelenVeri = "";
      }
    } else {
      // Karakteri buffer'a ekle
      gelenVeri += karakter;
    }
  }
}

/**
 * Gelen veriyi ayrıştırır
 * Format: SPEED:45.50,DIST:1.320,LAT:41.0082,LON:28.9784
 * NOT: LAT ve LON opsiyonel (gelmeyebilir)
 */
void veriAyristir(String veri) {
  veri.trim();
  
  // Çok kısa veri kontrolü (minimum: "SPEED:0,DIST:0" = 16 karakter)
  if (veri.length() < 16) {
    Serial.println("HATA: Veri çok kısa!");
    return;
  }

  // "SPEED:", ",DIST:", ",LAT:", ",LON:" etiketlerini bul
  int speedIndex = veri.indexOf("SPEED:");
  int distIndex = veri.indexOf(",DIST:");
  int latIndex = veri.indexOf(",LAT:");
  int lonIndex = veri.indexOf(",LON:");
  
  // SPEED ve DIST ZORUNLU
  if (speedIndex == -1 || distIndex == -1) {
    Serial.println("HATA: SPEED veya DIST etiketi bulunamadi!");
    Serial.print("Gelen veri: ");
    Serial.println(veri);
    return;
  }
  
  // SPEED >= -1 ve DIST > SPEED olmalı
  if (speedIndex >= distIndex) {
    Serial.println("HATA: Veri sırası yanlış!");
    return;
  }
  // Hız değerini al
  String hizStr = veri.substring(speedIndex + 6, distIndex);
  hiz = hizStr.toFloat();
  
  // Mesafe değerini al (LAT varsa ona kadar, yoksa sona kadar)
  String mesafeStr;
  if (latIndex != -1) {
    mesafeStr = veri.substring(distIndex + 6, latIndex);
  } else {
    // LAT yok, virgül varsa temizle
    mesafeStr = veri.substring(distIndex + 6);
    mesafeStr.trim();
    // Son karakter virgül ise kaldır
    if (mesafeStr.endsWith(",")) {
      mesafeStr = mesafeStr.substring(0, mesafeStr.length() - 1);
    }
  }
  mesafe = mesafeStr.toFloat();
  
  // Koordinatları al (varsa)
  if (latIndex != -1 && lonIndex != -1) {
    String enlemStr = veri.substring(latIndex + 5, lonIndex);
    enlem = enlemStr.toFloat();
    
    String boylamStr = veri.substring(lonIndex + 5);
    boylam = boylamStr.toFloat();
  }
  
  // Debug: Ayrıştırılan değerleri göster
  Serial.print("Ayristirildi -> Hiz: ");
  Serial.print(hiz, 1);
  Serial.print(" km/h | Mesafe: ");
  Serial.print(mesafe, 3); // 3 ondalık (1 metre hassasiyet)
  Serial.print(" km");
  if (latIndex != -1 && lonIndex != -1) {
    Serial.print(" | GPS: ");
    Serial.print(enlem, 6);
    Serial.print(", ");
    Serial.print(boylam, 6);
  }
  Serial.println();
}

/**
 * LCD ekranı günceller
 */
void lcdGuncelle() {
  // Bağlantı kesilmişse LCD'yi güncelleme
  if (sonDurum == "DISCONNECTED") {
    return;
  }
  
  // Hiç veri gelmediyse bekleme ekranı göster
  bool veriGelmis = (hiz != 0.0 || mesafe != 0.0 || enlem != 0.0 || boylam != 0.0);
  
  if (veriGelmis) {
    // Veri varsa göster
    String yeniDurum = "DATA";
    if (yeniDurum != sonDurum) {
      lcd.clear();
      sonDurum = yeniDurum;
    }
    
    // İlk satır: Hız
    lcd.setCursor(0, 0);
    lcd.print("Hiz:");
    
    // Hız değerini sağa hizalı göster
    lcd.setCursor(5, 0);
    if (hiz < 10) {
      lcd.print("  ");
    } else if (hiz < 100) {
      lcd.print(" ");
    }
    lcd.print(hiz, 1);
    lcd.print(" km/h ");
    
    // İkinci satır: Mesafe
    lcd.setCursor(0, 1);
    
    // Mesafe 1 km'den küçükse metre cinsinden göster (daha hassas)
    if (mesafe < 1.0) {
      int metrelik = (int)(mesafe * 1000);
      lcd.print("Mesafe: ");
      lcd.print(metrelik);
      lcd.print(" m    "); // Boşluklar ile temizlik
    } else {
      // 1 km ve üstü: km cinsinden 2 ondalık
      lcd.print("Mesafe:");
      lcd.print(mesafe, 2);
      lcd.print("km ");
    }
  } else if (sonDurum != "WAIT") {
    // Bağlantı bekleniyor ekranı (sadece hiç veri gelmediyse)
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("BT Bekleniyor...");
    lcd.setCursor(0, 1);
    lcd.print("TEST -> Test Modu");
    sonDurum = "WAIT";
  }
}

/**
 * Bluetooth bağlantı durumunu kontrol eder
 */
void baglantiKontrol() {
  unsigned long simdikiZaman = millis();
  
  // 8 saniyedir veri gelmiyorsa bağlantı kesilmiş sayılır
  if (bluetoothBagli && (simdikiZaman - sonVeriMillis > 8000)) {
    bluetoothBagli = false;
    
    // LCD'yi güncelle
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("BT Kesildi!");
    
    // GPS koordinatları varsa göster
    if (enlem != 0.0 || boylam != 0.0) {
      lcd.setCursor(0, 1);
      lcd.print(enlem, 3);  // 3 ondalık
      lcd.print(",");
      lcd.print(boylam, 3);
    }
    
    sonDurum = "DISCONNECTED";
    
    // Serial log
    Serial.println("\n=== BAGLANTI KESILDI ===");
    Serial.print("Son Hiz: ");
    Serial.print(hiz, 1);
    Serial.print(" km/h | Son Mesafe: ");
    Serial.print(mesafe, 2);
    Serial.println(" km");
    
    if (enlem != 0.0 || boylam != 0.0) {
      Serial.print("GPS Konum: ");
      Serial.print(enlem, 6);
      Serial.print(", ");
      Serial.println(boylam, 6);
    }
    Serial.println("========================\n");
  }
}

// ==================== TEST SENARYOSU ====================

// Test verileri Flash belleğe al (RAM'i korumak için)
const char testVeri0[] PROGMEM = "SPEED:0.00,DIST:0.000,LAT:41.036758,LON:28.985033";
const char testVeri1[] PROGMEM = "SPEED:15.50,DIST:0.040,LAT:41.037012,LON:28.985234";
const char testVeri2[] PROGMEM = "SPEED:32.80,DIST:0.120,LAT:41.037456,LON:28.985678";
const char testVeri3[] PROGMEM = "SPEED:45.20,DIST:0.280,LAT:41.038123,LON:28.986245";
const char testVeri4[] PROGMEM = "SPEED:52.00,DIST:0.510,LAT:41.038934,LON:28.987012";
const char testVeri5[] PROGMEM = "SPEED:48.70,DIST:0.780,LAT:41.039678,LON:28.987834";
const char testVeri6[] PROGMEM = "SPEED:35.40,DIST:1.020,LAT:41.040234,LON:28.988456";
const char testVeri7[] PROGMEM = "SPEED:25.10,DIST:1.180,LAT:41.040612,LON:28.988912";
const char testVeri8[] PROGMEM = "SPEED:12.30,DIST:1.280,LAT:41.040834,LON:28.989234";
const char testVeri9[] PROGMEM = "SPEED:0.00,DIST:1.320,LAT:41.041002,LON:28.989456";

const char* const testVerileri[] PROGMEM = {
  testVeri0, testVeri1, testVeri2, testVeri3, testVeri4,
  testVeri5, testVeri6, testVeri7, testVeri8, testVeri9
};

/**
 * Test senaryosu başlatır - sıralı GPS verileri gönderir
 * Bluetooth terminalinden "TEST" yazarak başlatın
 */
void testSenaryosuBaslat() {
  Serial.println("\n==============================");
  Serial.println("  TEST SENARYOSU BASLADI");
  Serial.println("==============================");
  Serial.println("10 adimlik GPS simulasyonu");
  Serial.println("Taksim -> 1.32 km mesafe");
  Serial.println("------------------------------\n");
  
  // Test modunu aktif et
  testModu = true;
  bluetoothBagli = true;
  sonVeriMillis = millis();
  
  // Buffer Flash'tan veri okumak için
  char buffer[80];
  
  int testSayisi = 10;
  
  for (int i = 0; i < testSayisi; i++) {
    // Flash'tan veriyi RAM'e kopyala
    strcpy_P(buffer, (char*)pgm_read_word(&(testVerileri[i])));
    
    Serial.print("[");
    Serial.print(i + 1);
    Serial.print("/");
    Serial.print(testSayisi);
    Serial.print("] ");
    Serial.println(buffer);
    
    // Veriyi işle
    veriAyristir(String(buffer));
    
    // Bağlantı zamanını güncelle
    sonVeriMillis = millis();
    
    // LCD'yi güncelle
    lcdGuncelle();
    
    // 1 saniye bekle
    delay(1000);
  }
  
  Serial.println("\n==============================");
  Serial.println("     TEST TAMAMLANDI!");
  Serial.println("==============================");
  Serial.println("Son degerler LCD'de gorunuyor");
  Serial.println("Yeni veri gonderebilirsiniz\n");
  
  // Test modundan çık ama değerleri koru
  testModu = false;
  // bluetoothBagli true kalsın, timeout 20 saniye olsun
  sonVeriMillis = millis() + 15000; // 15 saniye ek süre
}

