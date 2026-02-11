package com.serhat.dijitalgozluk.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Bluetooth Classic (HC-05/HC-06) bağlantı yöneticisi
 *
 * Bu sınıf, Arduino'ya bağlı HC-05 Bluetooth modülü ile
 * seri iletişim kurmayı sağlar.
 */
class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var lastDeviceAddress: String? = null

    // HC-05/HC-06 için standart Serial Port Profile UUID
    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BluetoothManager"
    }

    /**
     * Bluetooth desteği var mı kontrol eder
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Bluetooth açık mı kontrol eder
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Eşleştirilmiş Bluetooth cihazlarını listeler
     * Kotlin Flow ile reaktif döner
     */
    fun getPairedDevices(): Flow<List<BluetoothDeviceInfo>> = flow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothConnectPermission()) {
            emit(emptyList())
            return@flow
        }

        val pairedDevices = bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceInfo(
                name = device.name ?: "Unknown",
                address = device.address,
                isHC05 = device.name?.startsWith("HC-") == true
            )
        } ?: emptyList()

        emit(pairedDevices)
    }.flowOn(Dispatchers.IO)

    /**
     * Belirtilen Bluetooth cihazına bağlanır
     *
     * @param address Cihaz MAC adresi (örn: "00:11:22:33:44:55")
     * @param retryCount Bağlantı deneme sayısı
     * @return Başarılı bağlantı: true, Hata: false
     */
    suspend fun connect(address: String, retryCount: Int = 1): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothConnectPermission()) {
            return@withContext false
        }

        lastDeviceAddress = address

        repeat(retryCount.coerceAtLeast(1)) { attempt ->
            val success = connectInternal(address)
            if (success) return@withContext true
            if (attempt < retryCount - 1) {
                delay(500)
            }
        }

        false
    }

    /**
     * Son başarılı cihaza yeniden bağlanır
     */
    suspend fun reconnect(retryCount: Int = 3): Boolean {
        val address = lastDeviceAddress ?: return false
        return connect(address, retryCount)
    }

    /**
     * Bağlantı durumunu kontrol eder
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Bluetooth üzerinden veri gönderir
     *
     * @param data Gönderilecek String (örn: "SPEED:45.50,DIST:1.32\n")
     * @return Başarılı: true, Hata: false
     */
    suspend fun sendData(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                android.util.Log.w(TAG, "Not connected, cannot send data")
                return@withContext false
            }

            outputStream?.write(data.toByteArray())
            outputStream?.flush()

            android.util.Log.d(TAG, "Data sent: $data")
            true
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Send data failed: ${e.message}")
            disconnect()
            false
        }
    }

    /**
     * Bağlantıyı kapatır ve kaynakları temizler
     */
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()

            outputStream = null
            bluetoothSocket = null

            android.util.Log.d(TAG, "Disconnected successfully")
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Disconnect failed: ${e.message}")
        }
    }

    private fun connectInternal(address: String): Boolean {
        return try {
            val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(address)
                ?: return false

            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            false
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Bluetooth cihaz bilgilerini tutan data class
 */
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isHC05: Boolean = false
)

