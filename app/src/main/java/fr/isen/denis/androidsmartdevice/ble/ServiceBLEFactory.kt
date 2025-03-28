package fr.isen.denis.androidsmartdevice.ble

import fr.isen.denis.androidsmartdevice.ServiceBLE

object ServiceBLEFactory {
    private var instance: ServiceBLE? = null

    fun getServiceBLEInstance(): ServiceBLE {
        if (instance == null) {
            instance = ServiceBLE()
        }
        return instance!!
    }
}
