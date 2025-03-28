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

    val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
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
                    ledCharacteristic = gatt.services.getOrNull(2)?.characteristics?.getOrNull(0)
                    if (ledCharacteristic != null) {
                        Log.i("BLE", "Caractéristique LED trouvée")
                        onConnected()
                    } else {
                        Log.e("BLE", "Caractéristique LED non trouvée")
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val value = characteristic.value
                Log.i("NOTIFICATION", "Changement détecté : UUID=${characteristic.uuid}, valeur=${value.joinToString()}")
            }
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeLed(ledId: Int) {
        val characteristic = ledCharacteristic ?: run {
            Log.e("BLE", "Caractéristique LED non trouvée")
            return
        }

        val value = byteArrayOf(ledId.toByte())
        characteristic.value = value
        val result = bluetoothGatt?.writeCharacteristic(characteristic)
        Log.i("BLE", "Commande LED $ledId → succès=$result")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableNotificationForButton3() {
        val characteristic = bluetoothGatt
            ?.services?.getOrNull(3)
            ?.characteristics?.getOrNull(0)

        enableNotify(characteristic, "bouton 3")
    }

    private fun isNotifiable(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotify(characteristic: BluetoothGattCharacteristic?, label: String) {
        if (characteristic != null && isNotifiable(characteristic)) {
            bluetoothGatt?.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.descriptors.firstOrNull()
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)
            Log.i("BLE", "Notification activée pour $label")
        } else {
            Log.e("BLE", "Impossible d'activer la notification pour $label")
        }
    }
}
