package com.serhat.dijitalgozluk.presentation

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.serhat.dijitalgozluk.data.bluetooth.BluetoothDeviceInfo
import com.serhat.dijitalgozluk.data.bluetooth.BluetoothManager
import com.serhat.dijitalgozluk.data.gps.GPSManager
import com.serhat.dijitalgozluk.data.gps.LocationData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Ana ekran ViewModel
 * 
 * MVVM mimarisi ile state management
 * GPS ve Bluetooth işlemlerini koordine eder
 */
class SpeedViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)
    private val gpsManager = GPSManager(application)

    // UI State - LiveData ile
    private val _uiState = MutableLiveData(MainUiState())
    val uiState: LiveData<MainUiState> = _uiState

    // GPS location updates
    private var locationJob: Job? = null

    // Bluetooth veri gönderimi job
    private var sendDataJob: Job? = null

    init {
        refreshBluetoothState()
    }

    /**
     * Bluetooth desteği ve durumu kontrolü
     */
    fun refreshBluetoothState() {
        val isSupported = bluetoothManager.isBluetoothSupported()
        val isEnabled = bluetoothManager.isBluetoothEnabled()
        
        _uiState.value = _uiState.value?.copy(
            bluetoothSupported = isSupported,
            bluetoothEnabled = isEnabled
        )
    }

    /**
     * GPS durumunu güncelle
     */
    fun updateGpsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value?.copy(gpsEnabled = enabled)
    }

    /**
     * Eşleştirilmiş Bluetooth cihazlarını yükle
     */
    fun loadPairedDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoadingDevices = true)
            
            bluetoothManager.getPairedDevices()
                .catch { e ->
                    _uiState.value = _uiState.value?.copy(
                        isLoadingDevices = false,
                        error = "Cihazlar yüklenemedi: ${e.message}"
                    )
                }
                .collect { devices ->
                    _uiState.value = _uiState.value?.copy(
                        pairedDevices = devices,
                        isLoadingDevices = false
                    )
                }
        }
    }

    /**
     * Bluetooth cihazına bağlan
     */
    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isConnecting = true, error = null)
            
            val success = bluetoothManager.connect(deviceAddress, retryCount = 3)
            
            if (success) {
                _uiState.value = _uiState.value?.copy(
                    isConnected = true,
                    isConnecting = false,
                    connectedDeviceAddress = deviceAddress
                )
                
                // Bağlantı başarılı, GPS'i başlat
                startGPS()
                
                // Veri gönderimini başlat
                startDataTransmission()
            } else {
                _uiState.value = _uiState.value?.copy(
                    isConnecting = false,
                    error = "Bluetooth bağlantısı başarısız. HC-06 açık mı?"
                )
            }
        }
    }

    /**
     * Bluetooth bağlantısını kes
     */
    fun disconnect() {
        bluetoothManager.disconnect()
        stopGPS()
        stopDataTransmission()
        
        _uiState.value = _uiState.value?.copy(
            isConnected = false,
            connectedDeviceAddress = null,
            gpsActive = false
        )
    }

    /**
     * GPS konum güncellemelerini başlat
     */
    fun startGPS() {
        locationJob?.cancel()
        
        locationJob = viewModelScope.launch {
            gpsManager.getLocationUpdates()
                .catch { e ->
                    _uiState.value = _uiState.value?.copy(
                        error = "GPS hatası: ${e.message}",
                        gpsActive = false
                    )
                }
                .collect { locationData ->
                    _uiState.value = _uiState.value?.copy(
                        currentLocation = locationData,
                        gpsActive = true
                    )
                }
        }
    }

    /**
     * GPS'i durdur
     */
    private fun stopGPS() {
        locationJob?.cancel()
        locationJob = null
        
        _uiState.value = _uiState.value?.copy(gpsActive = false)
    }

    /**
     * Bluetooth veri gönderimini başlat (her 1 saniye)
     */
    private fun startDataTransmission() {
        sendDataJob?.cancel()
        
        sendDataJob = viewModelScope.launch {
            while (true) {
                val location = _uiState.value?.currentLocation
                
                if (location != null && bluetoothManager.isConnected()) {
                    val data = location.toBluetoothData()
                    val success = bluetoothManager.sendData(data)
                    
                    if (success) {
                        _uiState.value = _uiState.value?.copy(
                            lastDataSent = data,
                            dataSendCount = (_uiState.value?.dataSendCount ?: 0) + 1
                        )
                    } else {
                        // Bağlantı kopmuş, yeniden bağlanmayı dene
                        val reconnected = bluetoothManager.reconnect(retryCount = 2)
                        if (!reconnected) {
                            _uiState.value = _uiState.value?.copy(
                                isConnected = false,
                                error = "Bağlantı koptu"
                            )
                            stopDataTransmission()
                            stopGPS()
                        }
                    }
                }
                
                delay(1000) // 1 saniye bekle
            }
        }
    }

    /**
     * Veri gönderimini durdur
     */
    private fun stopDataTransmission() {
        sendDataJob?.cancel()
        sendDataJob = null
    }

    /**
     * Mesafeyi sıfırla
     */
    fun resetDistance() {
        gpsManager.resetDistance()
        _uiState.value = _uiState.value?.copy(
            currentLocation = _uiState.value?.currentLocation?.copy(totalDistanceKM = 0.0)
        )
    }

    /**
     * Hata mesajını temizle
     */
    fun clearError() {
        _uiState.value = _uiState.value?.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
        locationJob?.cancel()
        sendDataJob?.cancel()
    }
}

/**
 * UI State data class
 * 
 * Tüm UI durumunu tek bir immutable state'te tutar
 */
data class MainUiState(
    // Bluetooth
    val bluetoothSupported: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectedDeviceAddress: String? = null,
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val isLoadingDevices: Boolean = false,
    
    // GPS
    val gpsEnabled: Boolean = false,
    val gpsActive: Boolean = false,
    val currentLocation: LocationData? = null,
    
    // Data transmission
    val lastDataSent: String? = null,
    val dataSendCount: Int = 0,
    
    // Error handling
    val error: String? = null
) {
    /**
     * Format edilmiş hız
     */
    val formattedSpeed: String
        get() = currentLocation?.getFormattedSpeed() ?: "0.00"
    
    /**
     * Format edilmiş mesafe
     */
    val formattedDistance: String
        get() = currentLocation?.getFormattedDistance() ?: "0.00"
    
    /**
     * Bağlantı durumu metni
     */
    val connectionStatus: String
        get() = when {
            !bluetoothSupported -> "Bluetooth desteklenmiyor"
            !bluetoothEnabled -> "Bluetooth kapalı"
            isConnecting -> "Bağlanıyor..."
            isConnected -> "Bağlı ✓"
            else -> "Bağlı değil"
        }
}
