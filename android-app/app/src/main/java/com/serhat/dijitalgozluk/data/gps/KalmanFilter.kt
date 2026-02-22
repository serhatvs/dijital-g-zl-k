package com.serhat.dijitalgozluk.data.gps

/**
 * Basit Kalman filtresi implementasyonu
 * Tek boyutlu (ör. hız, mesafe, konum için)
 */
class KalmanFilter(
    private var processNoise: Float = 1f, // Q: Süreç gürültüsü
    private var measurementNoise: Float = 1f, // R: Ölçüm gürültüsü
    private var estimatedError: Float = 1f, // P: Başlangıç hata
    private var lastEstimate: Float = 0f // X: Başlangıç tahmin
) {
    fun update(measurement: Float): Float {
        // Kalman kazancı
        val kalmanGain = estimatedError / (estimatedError + measurementNoise)
        // Tahmin güncelle
        lastEstimate = lastEstimate + kalmanGain * (measurement - lastEstimate)
        // Hata güncelle
        estimatedError = (1 - kalmanGain) * estimatedError + processNoise
        return lastEstimate
    }

    fun reset(initialEstimate: Float = 0f, initialError: Float = 1f) {
        lastEstimate = initialEstimate
        estimatedError = initialError
    }
}
