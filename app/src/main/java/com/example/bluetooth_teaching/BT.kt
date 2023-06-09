package com.example.bluetooth_teaching

import android.Manifest
//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.*
import kotlin.concurrent.thread

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class Bluetooth (private val context: Context, private val activity: Activity){
    //設定一個資料class 讓他可以輸入name, address
    data class Item(val name: String, val address: String)
    companion object{
        var bytes: Int = 0
        private lateinit var printWriter: PrintWriter
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream
        private lateinit var bluetoothSocket: BluetoothSocket
        private lateinit var bluetoothServerSocket: BluetoothServerSocket
        private lateinit var mAdapter: BluetoothAdapter
        //取得"執行"權
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        //val BT_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        //val K_UUID:UUID = UUID.fromString(BT_UUID)
        const val DEVICE_NAME = "HC-05"
        val BUFFER = ByteArray(1024)
        var IP:String = ""
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun checkPermissions():Boolean  //取得權限
    {
        var fgBS: Boolean = false
        var fgS:Boolean = false
        var fgBC:Boolean = false
        var fgBA:Boolean = false
        var fgAFL:Boolean = false
        var fgACL:Boolean = false
        var fgALEC:Boolean = false
        var all = true
        var permission = mutableListOf<String>()

        fgBS = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        fgS = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        fgBC = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        fgBA = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN ) == PackageManager.PERMISSION_GRANTED
        fgAFL = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        fgACL = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        fgALEC = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS) == PackageManager.PERMISSION_GRANTED

        if(!fgBS){
            all = false
            permission.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if(!fgS){
            all = false
            permission.add(Manifest.permission.BLUETOOTH)
        }

        if(!fgBC){
            all = false
            permission.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if(!fgBA){
            all = false
            permission.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if(!fgAFL){
            all = false
            permission.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if(!fgACL){
            all = false
            permission.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if(!fgALEC){
            all = false
            permission.add(Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS)
        }

        if(permission.isNotEmpty())
        {
            Log.d("AAA","requiring permission...")
            ActivityCompat.requestPermissions(activity, permission.toTypedArray(), 1024)
        }
        return all
    }

    fun alert()                     //發出權限警報
    {
        AlertDialog.Builder(context).
        setTitle("取得權限").
        setMessage("權限未開啟，淺往應用程式資訊授予相關需要的權限").
        setPositiveButton("查詢"){_ ,_ ->
            applicationPermissions()
        }.
        setNegativeButton("取消"){_ ,_ ->
            Toast.makeText(context,"需要權限執行！請去開啟權限！", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun applicationPermissions()    //跳至應用程式資訊界面
    {
        val packageName = context.packageName // 取得目前應用程式的包名
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        activity.startActivity(intent)
    }

    //確認藍牙是否開啟 確認開啟，開始搜索裝置名稱
    fun btsearch()
    {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBluetoothIntent, 1)
            Toast.makeText(context,"請再次按下按鈕以進入連線畫面",Toast.LENGTH_SHORT).show()
        }else{
            searchDevice()
        }
    }

    private fun searchDevice()      //開始搜索並連接裝置
    {
        val receiver = MyReceiver()
        val pairedDevices = bluetoothAdapter!!.bondedDevices
        val device = pairedDevices.find {it.name == DEVICE_NAME }
        Log.d("aa",device.toString())
        if(device == null){
            Toast.makeText(context,"未有連線紀錄，進行搜尋搜尋到後進行連線" , Toast.LENGTH_SHORT).show()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
            }
            context.registerReceiver(receiver, filter)//receiver接收動作指定 前面設定接收的reciversaw 然後後面指定要的動作
            bluetoothAdapter.startDiscovery()
        }else{
            Toast.makeText(context,"進入連線畫面", Toast.LENGTH_SHORT).show()
            Intent(context, activity::class.java).apply {
                putExtra("address", device.toString())
                putExtra("blnserver","client")
                activity.startActivity(this)
            }
        }
    }

    fun serverOpen()                //開啟server端
    {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBluetoothIntent, 1)
            Toast.makeText(context,"請再次按下按鈕以進入連線畫面",Toast.LENGTH_SHORT).show()
        }else{
            Intent(context,MainActivity2::class.java).apply {
                putExtra("blnserver","server")
                activity.startActivity(this)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairDevices(list: ListView, bluetoothManager:BluetoothManager)//搜尋已配對過裝置，並選擇裝置進行連線
    {
        val listarray = mutableListOf("")
        val adapter = ArrayAdapter(context, R.layout.simple_list_item_1, R.id.text1, listarray)
        val itemrotate = " / "
        //只要有藍芽功能&&藍芽有開
        if(bluetoothAdapter != null&& bluetoothAdapter.isEnabled){
            val bluetoothAdapter = bluetoothManager.adapter
            val bondedDevices = bluetoothAdapter.bondedDevices
            for (device in bondedDevices) {
                val deviceName = device.name ?: "Unknown"                     // 處理
                val deviceHardwareAddress = device.address                    // MAC地址
                Log.d("deviceName",deviceName)
                Log.d("deviceHardwareAddress",deviceHardwareAddress.toString())
                val address = Item(deviceName, deviceHardwareAddress.toString())
                listarray.add(address.name + itemrotate + address.address)
                // 新增已配對的裝置清單
            }

            list.adapter = adapter
            list.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position) as String
                val addressnum = selectedItem.lastIndexOf(itemrotate)
                var addresstext = selectedItem.substring(addressnum + itemrotate.length )
                addresstext = addresstext.substring(0, 17)
                Log.d("Addresstext", addresstext)
                Intent(context, MainActivity2::class.java).apply {
                    putExtra("address", addresstext.toString())
                    putExtra("blnserver", "client")
                    activity.startActivity(this)
                }
            }
        }else{
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBluetoothIntent, 1)
            Toast.makeText(context,"請再次按下按鈕以進入連線畫面",Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun connection(address:String,textView: TextView) //進行client連線
    {
        IP = address
        val handler = Handler(Looper.getMainLooper())
        var bytes: Int
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice = mAdapter!!.getRemoteDevice(IP)//取得另一端address
        Log.d("aaaa",device.toString());
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        try {
            Thread{
                try {

                    //bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

                    bluetoothSocket.connect()

                    outputStream = bluetoothSocket.outputStream
                    inputStream = bluetoothSocket.inputStream
                    printWriter = PrintWriter(outputStream)

                    if(bluetoothSocket.isConnected){
                        handler.post{
                            textView.append("\n連線成功\n")
                        }
                    }

                    while (bluetoothSocket.isConnected) {
                        bytes = inputStream.read(BUFFER)
                        val data = String(BUFFER, 0, bytes)
                        val utf8Bytes = data.toByteArray(Charsets.UTF_8) // 轉換為UTF-8字節數組
                        val utf8String = String(utf8Bytes, Charsets.UTF_8) // 將UTF-8字節數組轉換為字符串
                        textView.append("SERVER：$utf8String")
                    }
                    Log.d("ERROR","ERROR")
                    if(!bluetoothSocket.isConnected){
                        bluetoothSocket.close()
                    }
                }catch (e:java.lang.Exception){
                    Log.d("AAA", e.message.toString())
                    handler.post{
                        Toast.makeText(context,"連線裝置失敗",Toast.LENGTH_SHORT).show()
                    }
                    Intent(context,MainActivity::class.java).apply {
                        bluetoothSocket.close()
                        activity.startActivity(this)
                    }
                }
            }.start()
        } catch (e: java.lang.Exception) {
            Log.d("AAA", e.message.toString())
            handler.post{
                Toast.makeText(context,"連線裝置失敗",Toast.LENGTH_SHORT).show()
            }
            Intent(context,MainActivity::class.java).apply {
                bluetoothSocket.close()
                activity.startActivity(this)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun acceptConnection(textView: TextView) //server端建立
    {
        Log.d("AAA","server")
        val handler = Handler(Looper.getMainLooper())
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        Thread{
            try
            {
                Log.d("AAA","a")
                bluetoothServerSocket = bluetoothAdapter!!.listenUsingInsecureRfcommWithServiceRecord(bluetoothAdapter.name, uuid)
                Log.d("AAA",uuid.toString())
                bluetoothSocket = bluetoothServerSocket.accept()
                Log.d("AAA","c")
                outputStream = bluetoothSocket.outputStream
                inputStream = bluetoothSocket.inputStream
                printWriter = PrintWriter(bluetoothSocket.outputStream)

                if(bluetoothSocket.isConnected){
                    handler.post{
                        textView.append("\n連線成功\n")
                    }
                }

                while (bluetoothSocket.isConnected) {
                    bytes = inputStream.read(BUFFER)
                    Log.d("AAA",bytes.toString())
                    val data = String(BUFFER, 0, bytes)
                    val utf8Bytes = data.toByteArray(Charsets.UTF_8) // 轉換為UTF-8字節數組
                    val utf8String = String(utf8Bytes, Charsets.UTF_8) // 將UTF-8字節數組轉換為字符串
                    textView.append("Client：$utf8String")
                }

            }
            catch (e:SecurityException)
            {
                Log.i("AAA", e.toString())
                handler.post{
                    Toast.makeText(context,"連線裝置失敗",Toast.LENGTH_SHORT).show()
                }
                Intent(context,MainActivity::class.java).apply {
                    bluetoothSocket.close()
                    activity.startActivity(this)
                }
            }
            Log.d("AAA","AAAA")
        }.start()

    }

    fun Close(){
        if(bluetoothSocket!=null)
            bluetoothSocket.close()
    }

    fun sendMessage(message: String) {
        printWriter.write(message+"\n")
        printWriter.flush()
    }
}