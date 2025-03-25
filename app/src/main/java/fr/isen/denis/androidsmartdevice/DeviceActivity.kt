package fr.isen.denis.androidsmartdevice

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class DeviceActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("device", BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("device")
        }

        setContent {
            MaterialTheme {
                device?.let {
                    DeviceScreen(it)
                } ?: Text("Aucun appareil reçu")
            }
        }
    }
}

@Composable
fun DeviceScreen(device: BluetoothDevice) {
    val context = LocalContext.current
    var connectionState by remember { mutableStateOf("Non connecté") }

    // Récupère nom et adresse avec vérification de permission
    val name = remember {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            device.name ?: "Nom inconnu"
        } else {
            "Nom non accessible"
        }
    }

    val address = remember {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            device.address
        } else {
            "Adresse non accessible"
        }
    }

    // Connexion BLE (si souhaitée)
    LaunchedEffect(device) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
            ) {
                connectionState = "Connexion en cours..."
                device.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt,
                        status: Int,
                        newState: Int
                    ) {
                        super.onConnectionStateChange(gatt, status, newState)
                        connectionState = if (newState == BluetoothGatt.STATE_CONNECTED) {
                            "Connecté à l'appareil"
                        } else {
                            "Déconnecté"
                        }
                    }
                })
            }
        } catch (e: SecurityException) {
            connectionState = "Permission manquante pour se connecter"
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Appareil sélectionné :", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Nom : $name")
        Text("Adresse : $address")
        Spacer(Modifier.height(16.dp))
        Text(connectionState)
    }
}
