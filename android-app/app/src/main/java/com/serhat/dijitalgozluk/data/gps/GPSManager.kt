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

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLocation: Location? = null
    private var totalDistance: Double = 0.0 // kilometre

    companion object {
        private const val TAG = "GPSManager"
        private const val UPDATE_INTERVAL = 1000L // 1 saniye
        private const val FASTEST_INTERVAL = 500L // Minimum 0.5 saniye
        private const val EARTH_RADIUS_KM = 6371.0
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
            setWaitForAccurateLocation(true)
        }.build()

        // LocationCallback
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val locationData = processLocation(location)
                    trySend(locationData)
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
    private fun processLocation(location: Location): LocationData {
        var speedKMH = 0f
        var distance = 0.0
        
        // Mesafe ve hız hesaplama
        if (lastLocation != null) {
            distance = calculateDistance(
                lastLocation!!.latitude,
                lastLocation!!.longitude,
                location.latitude,
                location.longitude
            )
            
            // Zaman farkı (saniye)
            val timeDiffSeconds = (location.time - lastLocation!!.time) / 1000.0
            
            // EMüLATÖR FİLTRESİ: Mantıksız sıçramaları yoksay
            // 1. Çok kısa zaman aralığı (< 0.3 saniye) - güncelleme çok hızlı
            // 2. Çok büyük mesafe (> 0.1 km = 100m) - teleportasyon
            val isSuspiciousJump = timeDiffSeconds < 0.3 || distance > 0.1
            
            if (!isSuspiciousJump && timeDiffSeconds > 0) {
                // Normal güncelleme - mesafe ekle
                totalDistance += distance
                
                // Hız hesapla (mesafe/zaman)
                if (distance > 0) {
                    // km / (s/3600) = km/h
                    val calculatedSpeed = ((distance / timeDiffSeconds) * 3600).toFloat()
                    // Maksimum hız sınırı: 200 km/h (emulator hatalarını engelle)
                    speedKMH = minOf(calculatedSpeed, 200f)
                }
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
            timestamp = location.time
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
    val timestamp: Long        // Unix timestamp (ms)
) {
    /**
     * Format edilmiş hız string
     */
    fun getFormattedSpeed(): String = "%.2f".format(speedKMH)

    /**
     * Format edilmiş mesafe string
     */
    fun getFormattedDistance(): String = "%.2f".format(totalDistanceKM)

    /**
     * Arduino'ya gönderilecek veri formatı
     * Format: SPEED:45.50,DIST:1.32,LAT:41.008240,LON:28.978359\n
     */
    fun toBluetoothData(): String {
        return "SPEED:${getFormattedSpeed()},DIST:${getFormattedDistance()},LAT:%.6f,LON:%.6f\n".format(latitude, longitude)
    }
}

