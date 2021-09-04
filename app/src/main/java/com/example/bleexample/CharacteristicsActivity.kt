package com.example.bleexample

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bleexample.ConnectionManager.CharacteristicValueReadListener
import com.example.bleexample.ConnectionManager.CharacteristicValueSubscribeListener
import com.example.bleexample.ui.CharacteristicAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CharacteristicsActivity : AppCompatActivity(), CharacteristicValueReadListener,
    CharacteristicValueSubscribeListener {


    lateinit var selectedCharacteristic: BluetoothGattCharacteristic
    lateinit var readBtn: FloatingActionButton
    lateinit var subBtn: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_characteristics)

        ConnectionManager.registerCharacteristicValueReadListener(this)
        ConnectionManager.registerCharacteristicValueSubscribeListener(this)

        val charList: MutableList<BluetoothGattCharacteristic> = mutableListOf()


        val charAdapter: CharacteristicAdapter by lazy {
            CharacteristicAdapter(charList) {
                selectedCharacteristic = it
                showOptions()

            }
        }

        val characteristicRecyView: RecyclerView = findViewById(R.id.characteristicRecyView)
        characteristicRecyView.apply {
            adapter = charAdapter
            layoutManager =
                LinearLayoutManager(this@CharacteristicsActivity, RecyclerView.VERTICAL, false)
        }

        val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        ConnectionManager.getAvaliableServices(device)?.forEach {
            it.characteristics.forEach {
                charList.add(it)
            }
        }

        charAdapter.notifyDataSetChanged()

        readBtn = findViewById(R.id.readBtn)
        subBtn = findViewById(R.id.subBtn)


        readBtn.setOnClickListener {
            ConnectionManager.readCharacteristicValue(selectedCharacteristic)
        }

        subBtn.setOnClickListener {

            ConnectionManager.subToCharacteristic(selectedCharacteristic)

        }

    }

    private fun showOptions() {
        readBtn.visibility = View.VISIBLE
        subBtn.visibility = View.VISIBLE
    }

    override fun onCharacteristicValeRead(characteristic: BluetoothGattCharacteristic) {
        runOnUiThread {

            Toast.makeText(applicationContext,characteristic.value.toHexString(),Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCharacteristicValueChanged(characteristic: BluetoothGattCharacteristic) {
        runOnUiThread {

            Toast.makeText(applicationContext,characteristic.value.toHexString(),Toast.LENGTH_SHORT).show()
        }
    }
}