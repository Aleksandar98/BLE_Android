package com.example.bleexample.ui

import android.bluetooth.BluetoothGattCharacteristic
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bleexample.R
import com.example.bleexample.utils.AllGattCharacteristics
import com.example.bleexample.utils.AllGattServices

class CharacteristicAdapter(
    val items : List<BluetoothGattCharacteristic>,
    val onClickListener: ((characteristic : BluetoothGattCharacteristic) -> Unit )
) : RecyclerView.Adapter<CharacteristicAdapter.ViewHolder>() {

    class ViewHolder(
        val itemView: View,
        val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
    ) : RecyclerView.ViewHolder(itemView) {

        val charTitle : TextView = itemView.findViewById(R.id.charTitle)

        fun bind ( characteristic : BluetoothGattCharacteristic ){
            charTitle.text = AllGattCharacteristics.getCharacteristicName(characteristic.uuid.toString())
            itemView.setOnClickListener { onClickListener.invoke(characteristic) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.characteristic_item,parent,false)

        return ViewHolder(view,onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}