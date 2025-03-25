package fr.isen.denis.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import android.content.Intent

class ScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanScreen()
        }
    }


}

@SuppressLint("ContextCastToActivity")
@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val bluetoothManager = remember {
        context.getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    }
    val bluetoothAdapter = bluetoothManager.adapter

    var isScanning by remember { mutableStateOf(false) }


    var isBluetoothAvailable by remember { mutableStateOf(true) }
    var isBluetoothEnabled by remember { mutableStateOf(true) }

    // Vérifie Bluetooth au lancement (faire demande popup)
    LaunchedEffect(Unit) {
        isBluetoothAvailable = bluetoothAdapter != null
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    }

    // Verif loca au lancement : OK
    val activity = LocalContext.current as Activity

    if (ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            2
        )
    }

    // Perm BT de scanner & connect
    if (ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
            3
        )
    }

    if (ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
            4
        )
    }




    // Liste factice de périphériques BLE
//    val fakeDevices = listOf(
//        "Appareil BLE - ISEN 1",
//        "Thermomètre BLE",
//        "Bracelet Connecté",
//        "ISEN Capteur Température",
//        "Device BLE Test"
//    )

    val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    val scanResults = remember { mutableStateListOf<BluetoothDevice>() }
    val scanCallback = remember {
        object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device.name != null && scanResults.none { it.address == device.address }) {
                    scanResults.add(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                println("Scan BLE failed: $errorCode")
            }
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            bluetoothLeScanner?.startScan(scanCallback)

            // Timeout auto après 20 secondes
            delay(20_000)
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
        } else {
            bluetoothLeScanner?.stopScan(scanCallback)
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Scan BLE",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when {
            !isBluetoothAvailable -> {
                Text(
                    "Erreur : Bluetooth non disponible sur cet appareil.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            !isBluetoothEnabled -> {
                Text(
                    "Veuillez activer le Bluetooth pour continuer.",
                    color = MaterialTheme.colorScheme.error
                )
            }


            else -> {
                val imageRes = if (isScanning) R.drawable.pause else R.drawable.play

                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = if (isScanning) "Pause Scan" else "Start Scan",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 24.dp)
                        .clickable { isScanning = !isScanning }
                )

                if (isScanning) {
                    Text("Périphériques détectés :", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (scanResults.isEmpty()) {
                        Text(
                            "Aucun appareil trouvé pour le moment...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyColumn {
                            items(scanResults) { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val intent = Intent(context, DeviceActivity::class.java)
                                            intent.putExtra("device", device)
                                            context.startActivity(intent)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = device.name ?: "Appareil inconnu", style = MaterialTheme.typography.bodyLarge)
                                        Text(text = "Adresse : ${device.address}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Scan non actif", style = MaterialTheme.typography.bodyMedium)
                }


            }

        }
    }


}


