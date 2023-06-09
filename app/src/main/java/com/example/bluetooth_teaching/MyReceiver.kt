package com.example.bluetooth_teaching

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyReceiver : BroadcastReceiver() {
    val DEVICE_NAME = "HC-05"
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action.toString() == BluetoothDevice.ACTION_FOUND)
        {
            val foundDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            val foundName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            if(foundName.toString() == DEVICE_NAME)
            {
                Log.d("foundDevice","found")
                Log.d("foundName",foundDevice.toString())
                val targetIntent =  Intent(context,MainActivity2::class.java)
                targetIntent.putExtra("address",foundDevice.toString())
                targetIntent.putExtra("blnserver","client")
                context?.startActivity(targetIntent)
                bluetoothAdapter!!.cancelDiscovery()
            }
        }
    }
}