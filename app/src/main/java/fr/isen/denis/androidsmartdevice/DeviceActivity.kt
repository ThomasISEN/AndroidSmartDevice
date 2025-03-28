package fr.isen.denis.androidsmartdevice

import android.Manifest
import android.bluetooth.BluetoothDevice
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
import fr.isen.denis.androidsmartdevice.ble.ServiceBLEFactory

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
    val bleService = remember { ServiceBLEFactory.getServiceBLEInstance() }
    var connected by remember { mutableStateOf(false) }

    LaunchedEffect(device) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            connectionState = "Connexion en cours..."
            bleService.connectToDevice(context, device) {
                connectionState = "Connecté à l'appareil"
                connected = true
            }
        } else {
            connectionState = "Permission manquante pour se connecter"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("État : $connectionState", style = MaterialTheme.typography.titleMedium)

        if (connected) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { bleService.writeLed(1) }) {
                Text("Allumer LED 1")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { bleService.writeLed(2) }) {
                Text("Allumer LED 2")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { bleService.writeLed(3) }) {
                Text("Allumer LED 3")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                bleService.enableNotificationForButton3()
            }) {
                Text("S'abonner au bouton 3")
            }

        }
    }
}