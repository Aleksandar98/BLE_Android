package com.example.bleexample.ui

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bleexample.R

class DeviceRecyAdapter(val dataSet: MutableList<ScanResult>,
                        private val onClickListener: ((device: ScanResult) -> Unit)
) : RecyclerView.Adapter<DeviceRecyAdapter.ViewHolder>() {


    class ViewHolder(
         itemView: View,
         val onClickListener: ((device: ScanResult) -> Unit)
    ) : RecyclerView.ViewHolder(itemView) {

        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceMacAdress: TextView = itemView.findViewById(R.id.deviceMACAdress)
        val signalStrength: TextView = itemView.findViewById(R.id.deviceSignalStr)

        fun bind(result: ScanResult){
            deviceName.text =  result.device.name
            deviceMacAdress.text =  result.device.address
            signalStrength.text =  result.rssi.toString()
            itemView.setOnClickListener { onClickListener.invoke(result) }
        }



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item,parent,false)

        return ViewHolder(view, onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(dataSet[position])

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}