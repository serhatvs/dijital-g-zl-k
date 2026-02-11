package com.serhat.dijitalgozluk.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.serhat.dijitalgozluk.R
import com.serhat.dijitalgozluk.databinding.ActivityMainBinding

/**
 * Ana Activity
 *
 * GPS hız ve mesafe verilerini gösterir
 * Bluetooth ile Arduino'ya veri gönderir
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SpeedViewModel by viewModels()

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "İzinler verildi", Toast.LENGTH_SHORT).show()
            viewModel.loadPairedDevices()
            viewModel.updateGpsEnabled(isLocationEnabled())
        } else {
            Toast.makeText(this, "İzinler gerekli!", Toast.LENGTH_LONG).show()
        }
    }

    // Bluetooth enable launcher
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth açıldı", Toast.LENGTH_SHORT).show()
            checkAndRequestPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBluetoothState()
        viewModel.updateGpsEnabled(isLocationEnabled())
    }

    private fun setupUI() {
        // Bluetooth cihaz seç butonu
        binding.btnSelectDevice.setOnClickListener {
            if (viewModel.uiState.value?.bluetoothEnabled == true) {
                showDeviceSelectionDialog()
            } else {
                requestEnableBluetooth()
            }
        }

        // Bağlan butonu
        binding.btnConnect.setOnClickListener {
            val devices = viewModel.uiState.value?.pairedDevices.orEmpty()
            if (devices.isNotEmpty()) {
                showDeviceSelectionDialog()
            } else {
                Toast.makeText(this, "Eşleştirilmiş cihaz yok", Toast.LENGTH_SHORT).show()
            }
        }

        // Bağlantıyı kes butonu
        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }

        // Mesafeyi sıfırla butonu
        binding.btnResetDistance.setOnClickListener {
            viewModel.resetDistance()
        }

        // Yenile butonu
        binding.btnRefresh.setOnClickListener {
            viewModel.loadPairedDevices()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            updateUI(state)
        }
    }

    private fun updateUI(state: MainUiState) {
        // Bağlantı durumu
        binding.tvConnectionStatus.text = state.connectionStatus
        binding.tvConnectionStatus.setTextColor(
            if (state.isConnected)
                ContextCompat.getColor(this, R.color.green)
            else
                ContextCompat.getColor(this, R.color.red)
        )

        // Hız
        binding.tvSpeed.text = state.formattedSpeed
        binding.tvSpeedUnit.text = "km/h"

        // Mesafe
        binding.tvDistance.text = state.formattedDistance
        binding.tvDistanceUnit.text = "km"

        // GPS durumu
        binding.tvGPSStatus.text = if (state.gpsActive) "GPS: Aktif ✓" else "GPS: Pasif"

        // Konum
        state.currentLocation?.let { location ->
            binding.tvLatitude.text = "Enlem: %.6f".format(location.latitude)
            binding.tvLongitude.text = "Boylam: %.6f".format(location.longitude)
            binding.tvAccuracy.text = "Hassasiyet: ±%.1f m".format(location.accuracy)
        } ?: run {
            binding.tvLatitude.text = "Enlem: -"
            binding.tvLongitude.text = "Boylam: -"
            binding.tvAccuracy.text = "Hassasiyet: -"
        }

        // Gönderilen veri sayısı
        binding.tvDataCount.text = "Gönderilen: ${state.dataSendCount}"

        // Buton durumları
        binding.btnConnect.isEnabled = !state.isConnected && !state.isConnecting
        binding.btnDisconnect.isEnabled = state.isConnected
        binding.btnSelectDevice.isEnabled = state.bluetoothEnabled

        // Hata mesajı
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        // Bluetooth izinleri (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        // Konum izinleri
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (requiredPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            // İzinler zaten verilmiş
            viewModel.loadPairedDevices()
            viewModel.updateGpsEnabled(isLocationEnabled())
        }
    }

    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun promptEnableLocation() {
        AlertDialog.Builder(this)
            .setTitle("Konum Kapalı")
            .setMessage("GPS verisi için konum servislerini açın.")
            .setPositiveButton("Ayarlar") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showDeviceSelectionDialog() {
        val devices = viewModel.uiState.value?.pairedDevices.orEmpty()

        if (devices.isEmpty()) {
            Toast.makeText(this, "Eşleştirilmiş cihaz bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = devices.map { device ->
            "${device.name}\n${device.address}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Bluetooth Cihazı Seç")
            .setItems(deviceNames) { _, which ->
                if (!isLocationEnabled()) {
                    promptEnableLocation()
                    return@setItems
                }
                val selectedDevice = devices[which]
                viewModel.connectToDevice(selectedDevice.address)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }
}

