# MIT APP INVENTOR - GPS HIZ VE MESAFE ÖLÇÜM UYGULAMASI

## 1. GİRİŞ

Bu rehber, MIT App Inventor kullanarak GPS verilerini Bluetooth ile Arduino'ya gönderen bir mobil uygulama oluşturmanızı sağlar.

**Gereksinimler:**
- Google hesabı (App Inventor için)
- Android telefon (test için)
- MIT AI2 Companion uygulaması (Google Play'den)

---

## 2. YENİ PROJE OLUŞTURMA

### Adım 1: App Inventor'a Giriş

1. https://appinventor.mit.edu adresine git
2. **Create Apps** butonuna tıkla
3. Google hesabınla giriş yap
4. **Start new project** → Proje adı: `GPSHizOlcer`

---

## 3. DESIGNER (TASARIM) MODU

### 3.1 Bileşenleri Ekle

#### A) Layout (Yerleşim)

**Palette → Layout → VerticalArrangement1**
- Width: Fill parent
- Height: Fill parent
- AlignHorizontal: Center
- AlignVertical: Top
- BackgroundColor: `#E3F2FD` (açık mavi)

#### B) User Interface (Kullanıcı Arayüzü)

**1. Label_Baslik (Başlık)**
```
Palette → User Interface → Label
Properties:
- Text: "GPS Hız Ölçer"
- FontSize: 24
- FontBold: true
- TextColor: #1976D2 (mavi)
- Width: Fill parent
- TextAlignment: center
```

**2. Label_Durum (Durum Göstergesi)**
```
Palette → User Interface → Label
Properties:
- Text: "Bluetooth: Bağlı Değil"
- FontSize: 14
- TextColor: #D32F2F (kırmızı)
- Width: Fill parent
```

**3. Button_BluetoothSec (Bluetooth Seçimi)**
```
Palette → User Interface → Button
Properties:
- Text: "Bluetooth Cihazı Seç"
- BackgroundColor: #2196F3 (mavi)
- FontSize: 16
- Width: Fill parent
```

**4. HorizontalArrangement1 (Bağlan Butonu için)**
```
Palette → Layout → HorizontalArrangement
Properties:
- Width: Fill parent
- AlignHorizontal: Center
```

İçine:
**Button_Baglan**
```
Text: "Bağlan"
BackgroundColor: #4CAF50 (yeşil)
Width: 150 pixels
```

**Button_BaglantiyiKes**
```
Text: "Kes"
BackgroundColor: #F44336 (kırmızı)
Width: 150 pixels
Visible: false (başlangıçta gizli)
```

**5. Label_Hiz (Hız Göstergesi)**
```
Text: "Hız: 0.0 km/h"
FontSize: 32
FontBold: true
TextColor: #FF6F00 (turuncu)
Width: Fill parent
TextAlignment: center
```

**6. Label_Mesafe (Mesafe Göstergesi)**
```
Text: "Mesafe: 0.00 km"
FontSize: 24
TextColor: #388E3C (yeşil)
Width: Fill parent
TextAlignment: center
```

**7. Label_GPS (GPS Durumu)**
```
Text: "GPS: Konum bekleniyor..."
FontSize: 12
TextColor: #757575 (gri)
Width: Fill parent
```

**8. Switch_OtomatikGonder (Otomatik Gönderim)**
```
Palette → User Interface → Switch
Properties:
- Text: "Otomatik Gönderim"
- Checked: true
- FontSize: 14
```

#### C) Sensors (Sensörler)

**1. LocationSensor1**
```
Palette → Sensors → LocationSensor
Properties:
- TimeInterval: 1000 (1 saniye)
- DistanceInterval: 0 (sürekli)
```

**2. Clock1 (Veri Gönderme Zamanlayıcısı)**
```
Palette → Sensors → Clock
Properties:
- TimerInterval: 1000 (1 saniye)
- TimerEnabled: false (başlangıçta kapalı)
```

#### D) Connectivity (Bağlantı)

**1. BluetoothClient1**
```
Palette → Connectivity → BluetoothClient
(Properties ayarı yok, otomatik)
```

**2. ListPicker_Cihazlar**
```
Palette → User Interface → ListPicker
Properties:
- Text: "Cihaz Seç"
- Visible: false (Button_BluetoothSec kullanacağız)
```

---

## 4. BLOCKS (KOD) MODU

### 4.1 Global Değişkenler

**Designer'dan Blocks'a geç** (sağ üst köşede Blocks butonu)

```blocks
// Global değişkenler oluştur

global hizMS (number) = 0
global hizKMH (number) = 0
global toplamMesafe (number) = 0
global eskiLat (number) = 0
global eskiLon (number) = 0
global bluetoothAdres (text) = ""
```

**Nasıl yapılır:**
1. **Built-in → Variables → initialize global name to**
2. Değişken adını değiştir (örn: `hizMS`)
3. Başlangıç değeri olarak **Math → 0** veya **Text → ""** tak

---

### 4.2 Screen Initialize (Ekran Başlangıcı)

```blocks
when Screen1.Initialize
do
  set Label_Durum.Text to "Bluetooth: Bağlı Değil"
  set Label_Durum.TextColor to -12627531  // Kırmızı
  set Label_Hiz.Text to "Hız: 0.0 km/h"
  set Label_Mesafe.Text to "Mesafe: 0.00 km"
  set Label_GPS.Text to "GPS: Konum bekleniyor..."
  set Clock1.TimerEnabled to false
```

---

### 4.3 Bluetooth Cihaz Seçimi

```blocks
when Button_BluetoothSec.Click
do
  set ListPicker_Cihazlar.Elements to BluetoothClient1.AddressesAndNames
  call ListPicker_Cihazlar.Open
```

```blocks
when ListPicker_Cihazlar.AfterPicking
do
  set global bluetoothAdres to (select list item: list = ListPicker_Cihazlar.Selection
                                                  index = 1)
  set Label_Durum.Text to join("Seçildi: ", ListPicker_Cihazlar.SelectionIndex)
```

**Açıklama:**
- `AddressesAndNames`: Eşleştirilmiş Bluetooth cihazlarını listeler
- `select list item index 1`: MAC adresini alır (format: "AA:BB:CC:DD:EE:FF Device Name")

---

### 4.4 Bluetooth Bağlantısı

```blocks
when Button_Baglan.Click
do
  if BluetoothClient1.IsConnected
  then
    set Label_Durum.Text to "Zaten bağlı!"
  else
    if call BluetoothClient1.Connect(address = global bluetoothAdres)
    then
      set Label_Durum.Text to "Bluetooth: Bağlı ✓"
      set Label_Durum.TextColor to -14503604  // Yeşil
      set Button_Baglan.Visible to false
      set Button_BaglantiyiKes.Visible to true
      set Clock1.TimerEnabled to true
    else
      set Label_Durum.Text to "Bağlantı Hatası!"
      call Notifier1.ShowAlert(message = "HC-06'ye bağlanılamadı. Cihazın açık olduğundan emin olun.")
```

```blocks
when Button_BaglantiyiKes.Click
do
  call BluetoothClient1.Disconnect
  set Label_Durum.Text to "Bluetooth: Bağlı Değil"
  set Label_Durum.TextColor to -12627531  // Kırmızı
  set Button_Baglan.Visible to true
  set Button_BaglantiyiKes.Visible to false
  set Clock1.TimerEnabled to false
```

**Not:** `Notifier1` eklemek için: **Palette → User Interface → Notifier**

---

### 4.5 GPS Veri Alma ve İşleme

```blocks
when LocationSensor1.LocationChanged
do (latitude, longitude, altitude, speed)
  // Hız hesaplama (m/s → km/h)
  set global hizMS to LocationSensor1.CurrentSpeed
  set global hizKMH to (global hizMS × 3.6)
  
  // Virgülden sonra 2 basamak
  set global hizKMH to round((global hizKMH × 100)) / 100
  
  // Mesafe hesaplama
  if (global eskiLat ≠ 0)
  then
    call Procedure_MesafeHesapla
  
  // Eski koordinatları kaydet
  set global eskiLat to latitude
  set global eskiLon to longitude
  
  // Ekranı güncelle
  set Label_Hiz.Text to join("Hız: ", global hizKMH, " km/h")
  set Label_Mesafe.Text to join("Mesafe: ", round(global toplamMesafe × 100) / 100, " km")
  set Label_GPS.Text to join("GPS: ", latitude, ", ", longitude)
```

---

### 4.6 Mesafe Hesaplama Procedure

```blocks
procedure MesafeHesapla
do
  // Haversine formülü ile iki nokta arası mesafe
  set local mesafeM to (call LocationSensor1.DistanceTo(
    latitude1 = global eskiLat,
    longitude1 = global eskiLon,
    latitude2 = LocationSensor1.Latitude,
    longitude2 = LocationSensor1.Longitude
  ))
  
  // metre → kilometre
  set global toplamMesafe to (global toplamMesafe + (mesafeM / 1000))
```

**Nasıl yapılır:**
1. **Built-in → Procedures → procedure**
2. Adını `MesafeHesapla` yap
3. İçine yukarıdaki blokları yerleştir

---

### 4.7 Bluetooth Veri Gönderme

```blocks
when Clock1.Timer
do
  if (BluetoothClient1.IsConnected and Switch_OtomatikGonder.Checked)
  then
    // Veri formatı: SPEED:45.50,DIST:1.32
    set local veriMetni to join(
      "SPEED:",
      round(global hizKMH × 100) / 100,
      ",DIST:",
      round(global toplamMesafe × 100) / 100,
      "\n"
    )
    
    // Bluetooth ile gönder
    call BluetoothClient1.SendText(text = veriMetni)
```

---

### 4.8 Hata Kontrolü ve GPS Durumu

```blocks
when LocationSensor1.StatusChanged
do (provider, status)
  if (status = "Available")
  then
    set Label_GPS.Text to "GPS: Hazır ✓"
    set Label_GPS.TextColor to -14503604  // Yeşil
  else if (status = "Out of Service")
  then
    set Label_GPS.Text to "GPS: Sinyal Yok ✗"
    set Label_GPS.TextColor to -12627531  // Kırmızı
  else if (status = "Temporarily Unavailable")
  then
    set Label_GPS.Text to "GPS: Bekleniyor..."
    set Label_GPS.TextColor to -30464  // Turuncu
```

---

## 5. EK ÖZELLİKLER (Opsiyonel)

### 5.1 Mesafe Sıfırlama

**Designer:**
```
Button_MesafeSifirla
Text: "Mesafeyi Sıfırla"
BackgroundColor: #FF9800
```

**Blocks:**
```blocks
when Button_MesafeSifirla.Click
do
  set global toplamMesafe to 0
  set Label_Mesafe.Text to "Mesafe: 0.00 km"
```

### 5.2 Hız Limiti Uyarısı

```blocks
when LocationSensor1.LocationChanged
do
  // ... (mevcut kodlar)
  
  // Hız limiti kontrolü
  if (global hizKMH > 50)
  then
    call Notifier1.ShowAlert(message = "DİKKAT: Hız limiti aşıldı!")
    call Player1.Start  // Ses uyarısı
```

**Gerekli:** **Palette → Media → Player** ekle ve `Player1.Source` → bir uyarı sesi yükle

### 5.3 Veri Kaydetme (Data Logging)

```blocks
// Clock1.Timer içine ekle
call File1.AppendToFile(
  fileName = "gps_kayit.csv",
  text = join(
    Clock1.Now,
    ",",
    global hizKMH,
    ",",
    global toplamMesafe,
    "\n"
  )
)
```

**Gerekli:** **Palette → Storage → File**

---

## 6. TEST ETME

### 6.1 AI2 Companion ile Test

1. **Connect → AI Companion**
2. Telefona MIT AI2 Companion uygulamasını yükle
3. QR kodu tara veya kodu gir
4. Uygulama telefonda açılır

### 6.2 Test Senaryoları

**Test 1: Bluetooth Bağlantısı**
1. Arduino'yu aç (HC-05 LED yanıp sönmeli)
2. Telefonda Bluetooth aç
3. Ayarlar → Bluetooth → HC-05 eşleştir (PIN: 1234)
4. Uygulamada "Bluetooth Cihazı Seç"
5. HC-05 seç ve "Bağlan"
6. Durum: "Bluetooth: Bağlı ✓" olmalı

**Test 2: GPS Test (Kapalı Alanda)**
1. Mock GPS uygulaması yükle (GPS Emulator)
2. Sahte konum ver (örn: Ankara)
3. Uygulama hız ve konum göstermeli

**Test 3: Gerçek Test (Açık Alanda)**
1. Dışarı çık (GPS sinyali için)
2. 30 saniye bekle (GPS fix)
3. Yürü veya araçla git
4. Arduino LCD'de hız görünmeli

---

## 7. APK OLUŞTURMA

### Adım 1: APK Build

1. **Build → App (save .apk to my computer)**
2. İndirmeyi bekle
3. `GPSHizOlcer.apk` dosyasını indir

### Adım 2: Telefona Yükleme

**Android:**
1. APK dosyasını telefona kopyala
2. Dosya yöneticisinden aç
3. "Bilinmeyen kaynaklardan yükleme" izni ver
4. Yükle

**Alternatif:** QR kod ile:
1. **Build → App (provide QR code for .apk)**
2. QR kodu telefon ile tara
3. Direkt indir ve yükle

---

## 8. İZİNLER

Uygulama şu izinleri gerektirir:

```
✓ Konum (GPS)
✓ Bluetooth
✓ İnternet (App Inventor için)
✓ Depolama (veri kaydetme için)
```

**Ayarlar → Uygulamalar → GPSHizOlcer → İzinler** → Tümünü aç

---

## 9. SORUN GİDERME

### GPS Çalışmıyor

**Çözüm:**
- Konum servisleri açık mı?
- Açık alanda mısınız?
- Mock locations kapalı mı?
- LocationSensor TimeInterval = 1000 ms olsun

### Bluetooth Bağlanmıyor

**Çözüm:**
- HC-06 eşleştirildi mi? (PIN: 1234)
- Arduino açık mı?
- MAC adresi doğru seçildi mi?
- Başka uygulama Bluetooth kullanmıyor mu?

### Veri Arduino'ya Gitmiyor

**Çözüm:**
- `Clock1.TimerEnabled = true` olmalı
- `Switch_OtomatikGonder.Checked = true` olmalı
- Arduino Serial Monitor'da veri görünüyor mu?
- Veri formatı doğru: `SPEED:X,DIST:Y\n`

### Uygulama Kapanıyor (Crash)

**Çözüm:**
- Blocks'da hata var mı? (kırmızı ünlem)
- Try-catch blokları ekle
- LocationSensor.LocationChanged içinde `if CurrentSpeed ≠ -1` kontrolü

---

## 10. BLOCKS KODU ÖZETİ

Tüm blocks kodu `blocks_kodu.txt` dosyasında detaylı olarak bulunmaktadır.

### Temel Akış:

```
Screen Initialize
  ↓
Button_BluetoothSec.Click → Cihaz listesi göster
  ↓
ListPicker seçimi → MAC adresi kaydet
  ↓
Button_Baglan.Click → Bluetooth bağlan
  ↓
LocationSensor.LocationChanged → GPS veri al
  ↓
Hız hesapla (m/s → km/h)
  ↓
Mesafe hesapla (Haversine)
  ↓
Clock1.Timer (1 saniyede bir)
  ↓
Bluetooth'a gönder: SPEED:X,DIST:Y
  ↓
Arduino LCD'de göster
```

---

## 11. EK KAYNAKLAR

- MIT App Inventor Docs: http://ai2.appinventor.mit.edu/reference/
- LocationSensor: http://ai2.appinventor.mit.edu/reference/components/sensors.html#LocationSensor
- BluetoothClient: http://ai2.appinventor.mit.edu/reference/components/connectivity.html#BluetoothClient
- Haversine Formula: https://en.wikipedia.org/wiki/Haversine_formula

---

## 12. PROJE DOSYASI

Bu rehberi takip ederek oluşturduğunuz projeyi `.aia` formatında kaydedebilirsiniz:

**Projects → Export selected project (.aia) to my computer**

Kayıtlı `.aia` dosyasını paylaşmak veya yedeklemek için kullanabilirsiniz.

**İçe aktarmak için:**
**Projects → Import project (.aia) from my computer**

---

**Tebrikler! GPS Hız ve Mesafe Ölçüm uygulamanız hazır! 🎉**
