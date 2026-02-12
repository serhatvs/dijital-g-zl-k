package com.serhat.dijitalgozluk.data.weather

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Sıcaklık veri yöneticisi
 * 
 * Hybrid yaklaşım:
 * 1. Telefon ambient sensörü (varsa)
 * 2. OpenWeatherMap API (internet varsa)
 * 3. Cached değer (offline fallback)
 */
class WeatherManager(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var ambientSensor: Sensor? = null
    private var currentAmbientTemp: Float? = null
    private var cachedWeatherTemp: Float? = null
    private var lastWeatherUpdate: Long = 0

    companion object {
        private const val TAG = "WeatherManager"
        
        // OpenWeatherMap API (ücretsiz, 1000 istek/gün)
        // Kayıt: https://openweathermap.org/api
        private const val WEATHER_API_KEY = "YOUR_API_KEY_HERE" // TODO: Gerçek key ekle
        private const val WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather"
        
        // Cache süresi: 10 dakika (API limiti için)
        private const val CACHE_DURATION_MS = 600_000L
    }

    init {
        // Ambient temperature sensörünü bul (varsa)
        ambientSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        
        if (ambientSensor != null) {
            Log.d(TAG, "Ambient temperature sensor found: ${ambientSensor?.name}")
        } else {
            Log.w(TAG, "Ambient temperature sensor not available - will use Weather API")
        }
    }

    /**
     * Ambient sensör dinlemeyi başlat
     */
    fun startAmbientSensorListening() {
        ambientSensor?.let { sensor ->
            sensorManager.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    currentAmbientTemp = event.values[0] // Celsius
                    Log.d(TAG, "Ambient sensor - Temperature: $currentAmbientTemp °C")
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    Log.d(TAG, "Sensor accuracy changed: $accuracy")
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            
            Log.d(TAG, "Ambient sensor listening started")
        }
    }

    /**
     * Ambient sensör dinlemeyi durdur
     */
    fun stopAmbientSensorListening() {
        ambientSensor?.let {
            sensorManager.unregisterListener(null as SensorEventListener?)
            Log.d(TAG, "Ambient sensor listening stopped")
        }
    }

    /**
     * Sıcaklık verisini al (hybrid yaklaşım)
     */
    suspend fun getTemperature(lat: Double, lon: Double): TemperatureData {
        // Öncelik 1: Telefon ambient sensörü (varsa)
        currentAmbientTemp?.let { temp ->
            Log.d(TAG, "Using ambient sensor: $temp °C")
            return TemperatureData(
                temp = temp,
                source = TemperatureSource.PHONE_SENSOR,
                timestamp = System.currentTimeMillis()
            )
        }

        // Öncelik 2: Hava durumu API (internet varsa ve cache eski)
        if (isOnline() && shouldUpdateWeather()) {
            val weatherTemp = fetchWeatherAPI(lat, lon)
            if (weatherTemp != null) {
                cachedWeatherTemp = weatherTemp
                lastWeatherUpdate = System.currentTimeMillis()
                Log.d(TAG, "Using Weather API: $weatherTemp °C")
                return TemperatureData(
                    temp = weatherTemp,
                    source = TemperatureSource.WEATHER_API,
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        // Öncelik 3: Cached hava durumu (10 dakika içinde alındıysa)
        cachedWeatherTemp?.let { temp ->
            val cacheAge = (System.currentTimeMillis() - lastWeatherUpdate) / 1000 / 60 // dakika
            Log.d(TAG, "Using cached weather: $temp °C (age: $cacheAge min)")
            return TemperatureData(
                temp = temp,
                source = TemperatureSource.CACHED,
                timestamp = lastWeatherUpdate
            )
        }

        // Hiçbir kaynak yok
        Log.w(TAG, "No temperature source available")
        return TemperatureData(
            temp = null,
            source = TemperatureSource.NONE,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * OpenWeatherMap API'den sıcaklık al
     */
    private suspend fun fetchWeatherAPI(lat: Double, lon: Double): Float? = withContext(Dispatchers.IO) {
        if (WEATHER_API_KEY == "YOUR_API_KEY_HERE") {
            Log.w(TAG, "Weather API key not configured")
            return@withContext null
        }

        try {
            val url = "$WEATHER_API_URL?lat=$lat&lon=$lon&appid=$WEATHER_API_KEY&units=metric"
            Log.d(TAG, "Fetching weather from API...")
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val temp = json.getJSONObject("main").getDouble("temp").toFloat()
            val feelsLike = json.getJSONObject("main").getDouble("feels_like").toFloat()
            val humidity = json.getJSONObject("main").getInt("humidity")
            val description = json.getJSONArray("weather").getJSONObject(0).getString("description")
            
            Log.d(TAG, "Weather API success: temp=$temp°C, feels=$feelsLike°C, humidity=$humidity%, desc=$description")
            
            return@withContext temp
        } catch (e: Exception) {
            Log.e(TAG, "Weather API error: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * İnternet bağlantısı kontrolü
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Weather API cache'i güncellemeli mi?
     */
    private fun shouldUpdateWeather(): Boolean {
        val timeSinceUpdate = System.currentTimeMillis() - lastWeatherUpdate
        return timeSinceUpdate > CACHE_DURATION_MS
    }
}

/**
 * Sıcaklık verisi
 */
data class TemperatureData(
    val temp: Float?,           // °C (null = bilinmiyor)
    val source: TemperatureSource,
    val timestamp: Long
)

/**
 * Sıcaklık kaynağı
 */
enum class TemperatureSource {
    PHONE_SENSOR,   // Telefon ambient sensörü
    WEATHER_API,    // OpenWeatherMap API
    CACHED,         // Cached weather data
    NONE            // Kaynak yok
}
