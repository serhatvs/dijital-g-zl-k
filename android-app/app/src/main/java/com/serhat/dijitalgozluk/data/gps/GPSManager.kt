package com.serhat.dijitalgozluk.data.gps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.serhat.dijitalgozluk.data.weather.WeatherManager
import java.util.Locale
import kotlin.math.*

/**
 * GPS konum ve hız takibi yöneticisi
 *
 * FusedLocationProviderClient kullanarak:
 * - Anlık konum
 * - Anlık hız (m/s)
 * - İki nokta arası mesafe (Haversine formülü)
 */
class GPSManager(private val context: Context) {
    // EKF entegrasyonu
    private val ekf = EKF(
        state = doubleArrayOf(0.0, 0.0, 0.0, 0.0),
        P = Array(4) { DoubleArray(4) { 1.0 } },
        Q = Array(4) { DoubleArray(4) { 0.01 } },
        R = Array(4) { DoubleArray(4) { 0.1 } }
    )

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val weatherManager = WeatherManager(context)

    private var lastLocation: Location? = null
    private var totalDistance: Double = 0.0 // kilometre
    
    // Kalman filtreleri
    private val speedKalman = KalmanFilter(processNoise = 0.5f, measurementNoise = 2f, estimatedError = 1f, lastEstimate = 0f)
    private val distanceKalman = KalmanFilter(processNoise = 0.01f, measurementNoise = 0.1f, estimatedError = 1f, lastEstimate = 0f)

    companion object {
        private const val TAG = "GPSManager"
        private const val UPDATE_INTERVAL = 100L // 100ms - saniyede 10 veri
        private const val FASTEST_INTERVAL = 100L // Minimum 100ms
        private const val BUFFER_SIZE = 10 // 10 örnek toplama (1 saniye)
        private const val EARTH_RADIUS_KM = 6371.0
        private const val MIN_DISTANCE_KM = 0.001 // 1 metre (küçük mesafe eşiği)
        private const val MAX_JUMP_DISTANCE_KM = 0.03 // 30 metre (teleportasyon filtresi)
        private const val MIN_TIME_DIFF_SECONDS = 0.5 // Minimum zaman farkı (hızlı güncellemeleri engelle)
    }

    /**
     * GPS konum güncellemelerini Flow olarak döner
     * Kotlin Flow ile reaktif veri akışı
     */
    fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        // İzin kontrolü
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close(IllegalStateException("Location permission not granted"))
            return@callbackFlow
        }

        // LocationRequest ayarları
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(0.5f) // 0.5 metre - küçük mesafeleri de yakala
            setWaitForAccurateLocation(true) // Yüksek doğruluk bekle
            setMaxUpdateDelayMillis(UPDATE_INTERVAL) // Maksimum gecikme
        }.build()

        // LocationCallback
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // GPS doğruluğu kontrolü
                    if (location.accuracy > 50f) {
                        android.util.Log.w(TAG, "Low GPS accuracy: ${location.accuracy}m - waiting for better signal")
                    }
                    // EKF predict (ivme yoksa 0 gir)
                    val dt = if (lastLocation != null) (location.time - lastLocation!!.time) / 1000.0 else 0.1
                    val ax = 0.0 // IMU yoksa 0
                    val ay = 0.0 // IMU yoksa 0
                    ekf.predict(dt, ax, ay)
                    // Ölçüm: [lat, lon, hız, eğim] (eğim yoksa 0)
                    val speed = if (location.hasSpeed()) location.speed.toDouble() else 0.0
                    val measurement = doubleArrayOf(location.latitude, location.longitude, speed, 0.0)
                    ekf.update(measurement)
                    val state = ekf.getState()
                    val ekfData = LocationData(
                        latitude = state[0],
                        longitude = state[1],
                        speedMS = state[2].toFloat(),
                        speedKMH = (state[2] * 3.6).toFloat(),
                        totalDistanceKM = locationData.totalDistanceKM, // mesafe klasik hesap
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        temperature = null,
                        temperatureSource = "NONE"
                    )
                    android.util.Log.d(TAG, "EKF: Lat ${state[0]}, Lon ${state[1]}, Speed ${state[2]}")
                    trySend(ekfData)
                    lastLocation = location
                }
            }
        }

        // Konum güncellemelerini başlat
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        android.util.Log.d(TAG, "Location updates started")

        // Flow kapandığında temizlik
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            android.util.Log.d(TAG, "Location updates stopped")
        }
    }

    /**
     * Location nesnesini LocationData'ya dönüştürür ve mesafe hesaplar
     */
    private suspend fun processLocation(location: Location): LocationData {
        var speedKMH = 0f
        
        // Mesafe ve hız hesaplama
        if (lastLocation != null) {
            val distance = calculateDistance(
                lastLocation!!.latitude,
                lastLocation!!.longitude,
                location.latitude,
                location.longitude
            )
            
            // Zaman farkı (saniye)
            val timeDiffSeconds = (location.time - lastLocation!!.time) / 1000.0
            
            // HASSAS FİLTRELEME:
            // 1. Çok kısa zaman aralığı (< 0.5 saniye) - güncelleme çok hızlı
            // 2. Çok büyük mesafe (> 30m) - teleportasyon veya GPS hatası
            val isTooFast = timeDiffSeconds < MIN_TIME_DIFF_SECONDS
            val isTeleportation = distance > MAX_JUMP_DISTANCE_KM
            
            if (!isTooFast && !isTeleportation && timeDiffSeconds > 0) {
                // Mesafeyi her durumda ekle (küçük mesafeler de önemli)
                totalDistance += distance
                
                // Hız hesaplama - sadece anlamlı mesafelerde (>1m)
                if (distance >= MIN_DISTANCE_KM) {
                    // km / (s/3600) = km/h
                    val calculatedSpeed = ((distance / timeDiffSeconds) * 3600).toFloat()
                    // Maksimum hız sınırı: 200 km/h
                    speedKMH = minOf(calculatedSpeed, 200f)
                    
                    android.util.Log.d(TAG, "Distance: ${distance * 1000}m, Speed: $speedKMH km/h, Accuracy: ${location.accuracy}m")
                } else {
                    // Çok küçük mesafe - önceki hızı koru
                    android.util.Log.d(TAG, "Distance too small: ${distance * 1000}m, keeping previous speed")
                }
            } else {
                if (isTooFast) android.util.Log.d(TAG, "Update too fast: ${timeDiffSeconds}s")
                if (isTeleportation) android.util.Log.d(TAG, "Teleportation detected: ${distance * 1000}m")
            }
        }
        
        // Eğer location.speed varsa tercih et (gerçek cihazlarda daha doğru)
        val speedMS = if (location.hasSpeed() && location.speed > 0) {
            location.speed
        } else {
            (speedKMH / 3.6).toFloat()
        }
        
        // Hızı güncelle
        if (location.hasSpeed() && location.speed > 0) {
            speedKMH = minOf((speedMS * 3.6).toFloat(), 200f)
        }

        lastLocation = location

        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            speedMS = speedMS,
            speedKMH = speedKMH,
            totalDistanceKM = totalDistance,
            accuracy = location.accuracy,
            timestamp = location.time,
            temperature = tempData.temp,
            temperatureSource = tempData.source.name
        )
    }

    /**
     * İki GPS koordinatı arasındaki mesafeyi hesaplar (Haversine formülü)
     *
     * @return Mesafe (kilometre)
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Toplam mesafeyi sıfırlar
     */
    fun resetDistance() {
        totalDistance = 0.0
        lastLocation = null
        android.util.Log.d(TAG, "Distance reset")
    }

    /**
     * Son bilinen konumu al (tek seferlik)
     */
    suspend fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return fusedLocationClient.lastLocation.await()
    }
}

/**
 * GPS konum verilerini tutan data class
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speedMS: Float,        // Hız (m/s)
    val speedKMH: Float,       // Hız (km/h)
    val totalDistanceKM: Double, // Toplam mesafe (km)
    val accuracy: Float,       // Konum hassasiyeti (metre)
    val timestamp: Long,       // Unix timestamp (ms)
    val temperature: Float? = null,     // Sıcaklık (°C, null = bilinmiyor)
    val temperatureSource: String = "NONE"  // Kaynak: PHONE_SENSOR, WEATHER_API, CACHED, NONE
) {
    /**
     * Format edilmiş hız string
     * Locale.US kullanarak her zaman nokta (.) ondalık ayırıcısı kullanır
     */
    fun getFormattedSpeed(): String = String.format(Locale.US, "%.2f", speedKMH)

    /**
     * Format edilmiş mesafe string
     * Locale.US kullanarak her zaman nokta (.) ondalık ayırıcısı kullanır
     */
    fun getFormattedDistance(): String = String.format(Locale.US, "%.3f", totalDistanceKM)

    /**
     * Arduino'ya gönderilecek veri formatı
     * Format: SPEED:45.50,DIST:1.320,LAT:41.008240,LON:28.978359,TEMP:-8.5\n
     * Distance: 3 ondalık (1 metre hassasiyet)
     * LAT/LON: 6 ondalık (0.11 metre hassasiyet)
     * TEMP: Opsiyonel (1 ondalık)
     * 
     * ÖNEMLİ: Locale.US kullanılarak her zaman nokta (.) ondalık ayırıcısı kullanılır.
     * Türkiye gibi vergül (,) kullanan ülkelerde bile Arduino nokta bekliyor!
     */
    fun toBluetoothData(): String {
        val baseData = String.format(
            Locale.US,
            "SPEED:%s,DIST:%s,LAT:%.6f,LON:%.6f",
            getFormattedSpeed(),
            getFormattedDistance(),
            latitude,
            longitude
        )
        
        // Sıcaklık varsa ekle (opsiyonel)
        val tempData = temperature?.let { temp ->
            String.format(Locale.US, ",TEMP:%.1f", temp)
        } ?: ""
        
        return "$baseData$tempData\n"
    }
}

