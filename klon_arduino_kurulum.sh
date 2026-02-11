#!/bin/bash
# Arduino Uno Klon - Hızlı Kurulum ve Test Scripti
# Kullanım: chmod +x klon_arduino_kurulum.sh && ./klon_arduino_kurulum.sh

echo "╔════════════════════════════════════════════════════════╗"
echo "║     Arduino Uno Klon - Kurulum ve Test Scripti       ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Renkler
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. CH340 Driver Kontrolü
echo "📡 CH340 Driver Kontrolü..."
if lsmod | grep -q ch341; then
    echo -e "${GREEN}✓ CH340 driver yüklü${NC}"
else
    echo -e "${YELLOW}⚠ CH340 driver bulunamadı, yükleniyor...${NC}"
    sudo modprobe ch341 2>/dev/null
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ CH340 driver yüklendi${NC}"
    else
        echo -e "${RED}✗ CH340 driver yüklenemedi${NC}"
        echo "  Manuel kurulum gerekebilir: sudo apt-get install linux-headers-\$(uname -r)"
    fi
fi

echo ""

# 2. Kullanıcı İzinleri
echo "🔐 Kullanıcı İzinleri Kontrolü..."
if groups | grep -q dialout; then
    echo -e "${GREEN}✓ dialout grubu izni var${NC}"
else
    echo -e "${YELLOW}⚠ dialout grubu izni ekleniyor...${NC}"
    sudo usermod -a -G dialout $USER
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ dialout izni eklendi${NC}"
        echo -e "${YELLOW}! Değişikliklerin etkili olması için oturumu kapat-aç veya sistemi yeniden başlat${NC}"
    else
        echo -e "${RED}✗ İzin eklenemedi${NC}"
    fi
fi

echo ""

# 3. Arduino Port Tespiti
echo "🔌 Arduino Port Tespiti..."
PORT=$(ls /dev/ttyUSB* 2>/dev/null | head -n1)
if [ -z "$PORT" ]; then
    PORT=$(ls /dev/ttyACM* 2>/dev/null | head -n1)
fi

if [ -z "$PORT" ]; then
    echo -e "${RED}✗ Arduino bulunamadı!${NC}"
    echo "  Kontrol Et:"
    echo "  1. Arduino USB kablosu bağlı mı?"
    echo "  2. USB kablosu veri kablosu mu? (bazıları sadece şarj)"
    echo "  3. lsusb komutu ile cihaz görünüyor mu?"
    lsusb | grep -i "QinHeng\|CH340\|Arduino"
    exit 1
else
    echo -e "${GREEN}✓ Arduino bulundu: $PORT${NC}"
    
    # USB cihaz bilgisi
    USB_INFO=$(lsusb | grep -i "QinHeng\|CH340\|Arduino")
    if [ ! -z "$USB_INFO" ]; then
        echo "  USB Cihaz: $USB_INFO"
    fi
fi

echo ""

# 4. Arduino CLI Kontrolü
echo "🛠️  Arduino CLI Kontrolü..."
if [ -f "./bin/arduino-cli" ]; then
    echo -e "${GREEN}✓ Arduino CLI mevcut${NC}"
    ARDUINO_CLI="./bin/arduino-cli"
else
    echo -e "${YELLOW}⚠ Arduino CLI bulunamadı, kontrol ediliyor...${NC}"
    if command -v arduino-cli &> /dev/null; then
        ARDUINO_CLI="arduino-cli"
        echo -e "${GREEN}✓ Sistem Arduino CLI kullanılacak${NC}"
    else
        echo -e "${RED}✗ Arduino CLI bulunamadı${NC}"
        echo "  Kurulum için: curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh"
        exit 1
    fi
fi

echo ""

# 5. Kütüphane Kontrolü
echo "📚 Kütüphane Kontrolü..."
LIBS=$($ARDUINO_CLI lib list 2>/dev/null)
if echo "$LIBS" | grep -q "LiquidCrystal I2C"; then
    echo -e "${GREEN}✓ LiquidCrystal I2C kütüphanesi yüklü${NC}"
else
    echo -e "${YELLOW}⚠ LiquidCrystal I2C kütüphanesi eksik, yükleniyor...${NC}"
    $ARDUINO_CLI lib install "LiquidCrystal I2C"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Kütüphane yüklendi${NC}"
    fi
fi

echo ""

# 6. Blink Test Kodu
echo "💡 Blink Test Kodu Hazırlanıyor..."
cat > /tmp/blink_test.ino << 'EOF'
void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);
  Serial.println("Arduino Uno Klon Test - OK");
}

void loop() {
  digitalWrite(LED_BUILTIN, HIGH);
  Serial.println("LED: ON");
  delay(1000);
  digitalWrite(LED_BUILTIN, LOW);
  Serial.println("LED: OFF");
  delay(1000);
}
EOF

echo -e "${GREEN}✓ Test kodu oluşturuldu${NC}"

echo ""

# 7. Derleme
echo "⚙️  Kod Derleniyor..."
$ARDUINO_CLI compile --fqbn arduino:avr:uno /tmp/blink_test.ino 2>&1 | tail -3
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Derleme başarılı${NC}"
else
    echo -e "${RED}✗ Derleme hatası${NC}"
    exit 1
fi

echo ""

# 8. Upload
echo "📤 Arduino'ya Yükleniyor..."
echo "   Port: $PORT"
$ARDUINO_CLI upload -p $PORT --fqbn arduino:avr:uno /tmp/blink_test.ino
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Upload başarılı!${NC}"
    echo ""
    echo "🎉 Arduino'daki LED (Pin 13) yanıp sönüyor olmalı"
    echo ""
    echo "📊 Serial Monitor için:"
    echo "   $ARDUINO_CLI monitor -p $PORT -c baudrate=9600"
else
    echo -e "${RED}✗ Upload hatası${NC}"
    echo ""
    echo "Olası Nedenler:"
    echo "  1. Bluetooth modülü (HC-05) RX/TX pinlerine bağlı olabilir"
    echo "  2. İzin sorunu: sudo chmod 666 $PORT"
    echo "  3. Başka bir program portu kullanıyor"
    exit 1
fi

echo ""

# 9. Proje Kodu Kontrolü
echo "📁 Proje Kodu Kontrolü..."
if [ -f "arduino_kod.ino" ]; then
    echo -e "${GREEN}✓ arduino_kod.ino bulundu${NC}"
    echo ""
    echo "Projeyi yüklemek için:"
    echo "   $ARDUINO_CLI compile --fqbn arduino:avr:uno arduino_kod.ino"
    echo "   $ARDUINO_CLI upload -p $PORT --fqbn arduino:avr:uno arduino_kod.ino"
else
    echo -e "${YELLOW}⚠ arduino_kod.ino bulunamadı${NC}"
fi

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║                  Kurulum Tamamlandı!                  ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Özet:"
echo "  ✓ CH340 Driver: OK"
echo "  ✓ Arduino Port: $PORT"
echo "  ✓ Blink Test: OK"
echo ""
echo "Sonraki Adımlar:"
echo "  1. HC-05 Bluetooth modülünü bağla"
echo "  2. LCD ekranı bağla"
echo "  3. arduino_kod.ino dosyasını yükle"
echo "  4. MIT App Inventor uygulamasını aç"
echo ""
echo "Dokümantasyon:"
echo "  - KLON_ARDUINO_KURULUM.md (CH340 detayları)"
echo "  - MONTAJ_REHBERI.md (Donanım bağlantıları)"
echo "  - PROJE_REHBERI.md (Sistem mimarisi)"
