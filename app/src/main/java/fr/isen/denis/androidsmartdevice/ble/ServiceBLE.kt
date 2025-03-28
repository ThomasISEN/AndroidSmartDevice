package fr.isen.denis.androidsmartdevice

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission

class ServiceBLE {

    private var bluetoothGatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private val ledStates = mutableMapOf(1 to false, 2 to false, 3 to false)

    val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(context: Context, device: BluetoothDevice, onConnected: () -> Unit) {
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BLE", "Connecté à l'appareil")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BLE", "Déconnecté de l'appareil")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("BLE", "Services découverts")
                    try {
                        val service = gatt.services[2]
                        val characteristic = service.characteristics[0]
                        ledCharacteristic = characteristic
                        Log.i("BLE", "Caractéristique LED trouvée (services[2].characteristics[0])")
                        onConnected()
                    } catch (e: Exception) {
                        Log.e("BLE", "Erreur accès caractéristique LED : ${e.message}")
                    }
                }
            }
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeLed(ledId: Int) {
        val gatt = bluetoothGatt
        val characteristic = ledCharacteristic

        if (gatt == null || characteristic == null) {
            Log.e("BLE", "Impossible d’écrire : GATT ou caractéristique null")
            return
        }

        val isOn = ledStates[ledId] ?: false
        val newValue = if (isOn) 0x00 else ledId

        // Reset tous les états locaux
        ledStates.keys.forEach { ledStates[it] = false }

        // Active cette LED si elle est allumée
        if (!isOn) {
            ledStates[ledId] = true
        }

        characteristic.value = byteArrayOf(newValue.toByte())
        val success = gatt.writeCharacteristic(characteristic)
        Log.i("BLE", "Écriture LED $ledId → value=$newValue, success=$success")
    }

}
