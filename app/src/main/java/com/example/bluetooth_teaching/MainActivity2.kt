package com.example.bluetooth_teaching

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.bluetooth_teaching.databinding.ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    val bt = Bluetooth(this,this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)


        val IP = intent.getStringExtra("address").toString()
        val blnserver = intent.getStringExtra("blnserver").toString()
        Log.d("blnserver",blnserver)
        Log.d("blnserver",IP)
        if (blnserver== "server"){
            bt.acceptConnection(binding.textView)
        }
        else
        {
            bt.connection(IP.toString(),binding.textView)
        }


        binding.button.setOnClickListener{
            bt.sendMessage(binding.edittext.text.toString()+"\n")
            binding.textView.append("USER："+binding.edittext.text.toString()+"\n")
            binding.edittext.text.clear()
        }
    }

    override fun onStop() {
        super.onStop()
        bt.Close()
    }
}