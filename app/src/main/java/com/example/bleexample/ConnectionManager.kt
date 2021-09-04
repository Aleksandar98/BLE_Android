package com.example.bleexample

import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.bleexample.ui.DeviceRecyAdapter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ConnectionManager {

    private val deviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var connectionCompletedCallback: ConnectionCompletedListener
    private lateinit var characteristicValueCallback: CharacteristicValueReadListener
    private lateinit var characteristicValueChangedCallback: CharacteristicValueSubscribeListener

    fun registerConnectionCompletedListener( callback: ConnectionCompletedListener){
        connectionCompletedCallback = callback
    }

    fun unregisterConnectionCompletedListener(callback: ConnectionCompletedListener){

    }


    fun registerCharacteristicValueReadListener(callback: CharacteristicValueReadListener){
        characteristicValueCallback = callback
    }

    fun unregisterCharacteristicValueReadListener(callback: CharacteristicValueReadListener){
        characteristicValueCallback = callback
    }


    fun registerCharacteristicValueSubscribeListener(callback: CharacteristicValueSubscribeListener){
        characteristicValueChangedCallback = callback
    }

    fun unregisterCharacteristicValueSubscribeListener(callback: CharacteristicValueSubscribeListener){
        characteristicValueChangedCallback = callback
    }



    fun getAvaliableServices( btDevice: BluetoothDevice): List<BluetoothGattService>?{
        return deviceGattMap[btDevice]?.services
    }

    fun readBatteryLevel() {
        val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val batteryLevelChar = bluetoothGatt!!
            .getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)
        if (batteryLevelChar?.isReadable() == true) {
            bluetoothGatt!!.readCharacteristic(batteryLevelChar)
        }
    }

    fun readCharacteristicValue(characteristic: BluetoothGattCharacteristic){
        if (characteristic?.isReadable() == true) {
            bluetoothGatt?.readCharacteristic(characteristic)
        }
    }
    fun subToCharacteristic(characteristic: BluetoothGattCharacteristic){
            if (characteristic != null) {
                enableNotifications(characteristic)
            }
        }



     val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    // TODO: Store a reference to BluetoothGatt
                    bluetoothGatt = gatt

                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here
                deviceGattMap[gatt.device] = gatt
                connectionCompletedCallback.onConnectionCompleted(gatt)


            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                        Log.i("BluetoothGattCallback", "Read characteristic percentage  $uuid:\n${value.first().toInt()}")
                        characteristicValueCallback.onCharacteristicValeRead(characteristic)
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value.toHexString()}")
                characteristicValueChangedCallback.onCharacteristicValueChanged(characteristic)
            }
        }
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun subToBatteryLvlChar() {
        val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val batteryLevelChar = bluetoothGatt!!
            .getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)

        if (batteryLevelChar != null) {
            enableNotifications(batteryLevelChar)
        }
    }

    interface ConnectionCompletedListener{

        fun onConnectionCompleted(gatt: BluetoothGatt)
    }

    interface CharacteristicValueReadListener{

        fun onCharacteristicValeRead(characteristic: BluetoothGattCharacteristic)
    }

    interface CharacteristicValueSubscribeListener{

        fun onCharacteristicValueChanged(characteristic: BluetoothGattCharacteristic)
    }


}