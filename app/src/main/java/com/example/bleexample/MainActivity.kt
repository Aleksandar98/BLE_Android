package com.example.bleexample

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.se.omapi.SEService
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bleexample.ui.DeviceRecyAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import kotlin.collections.HashMap


const val TAG = "myTag"
private var isScanning = false

class MainActivity : AppCompatActivity(), ConnectionManager.ConnectionCompletedListener {

    //Initialization

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = ContextCompat.getSystemService(this, BluetoothManager::class.java) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: DeviceRecyAdapter by lazy {
        DeviceRecyAdapter(scanResults) { result ->
            if(isScanning)
                stopBleScan()
            with(result.device){
                Log.d(TAG, "Connectiong to : $address")
                connectGatt(this@MainActivity,false, ConnectionManager.gattCallback)

            }
        }
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()




    // CallBacks



    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }


    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val noDevicesTxt : TextView = findViewById(R.id.noDevicesTxt)
        if(scanResults.size == 0 ) noDevicesTxt.visibility = View.GONE


        ConnectionManager.registerConnectionCompletedListener(this)
        initFloatingButtons()
        initRecyclerView()

    }

    private fun initFloatingButtons() {
        val scanBtn: FloatingActionButton = findViewById(R.id.scanBtn)


        scanBtn.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    private fun initRecyclerView() {
        val deviceRecyclerView: RecyclerView = findViewById(R.id.deviceRecyclerView)

        deviceRecyclerView.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        deviceRecyclerView.adapter = scanResultAdapter
    }


    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    private fun startBleScan() {

        Log.d(TAG, "startBleScan: lista mi je: "+scanResultAdapter.dataSet.size)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        }
        else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }




    // region permissions
    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location permission required")
        builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted location access in order to scan for BLE devices.")

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        builder.show()
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    //endregion

    fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onConnectionCompleted(gatt: BluetoothGatt) {
//                        val map : HashMap<BluetoothGattService,BluetoothGattCharacteristic> = HashMap()
//                        gatt.services.forEach { service ->
//                            service.characteristics.forEach { characteristic ->
//                                map.put(service,characteristic)
//                            }
//                        }

                        Intent(application,CharacteristicsActivity::class.java).also {
                        it.putExtra(BluetoothDevice.EXTRA_DEVICE,gatt.device)
                        startActivity(it)
                    }
    }

}