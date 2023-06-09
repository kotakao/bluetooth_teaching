package com.example.bluetooth_teaching
//noinspection SuspiciousImport
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.example.bluetooth_teaching.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private var bt = Bluetooth(this,this)

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //取得藍芽"管理"權限 (取得系統中藍芽使用相關權限) 建議在activity使用，寫在class裡面你拿不到(很難拿)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bt.checkPermissions()               //檢查權限

        binding.start.setOnClickListener{
            if(switch1.isChecked)//如果是server
            {
                bt.serverOpen()
            }
            else
            {
                bt.getPairDevices(address, bluetoothManager)
            }
        }
        binding.search.setOnClickListener{
            if(bt.checkPermissions()){
                //開始搜尋裝置
                bt.btsearch()
            }else{
                //開啟程式資訊提示框
                bt.alert()
            }
        }
    }


    /*
    布局適配器
    ArrayAdapter(
    context,
    android.R.layout.simple_list_item_1：一個單行的佈局，只包含一個 TextView，適用於只顯示單行文字的 ListView。
    android.R.layout.simple_list_item_2：一個雙行的佈局，包含兩個 TextView，通常用於顯示一個主要文字和一個次要文字的 ListView。
    android.R.layout.simple_spinner_item：一個用於 Spinner 的簡單單行佈局，只包含一個 TextView。
    android.R.layout.simple_dropdown_item_1line：用於 AutoCompleteTextView 的佈局，一個單行的下拉列表項目佈局。
    ,
    android.R.id.text1：在預設的佈局資源 android.R.layout.simple_list_item_1 和 android.R.layout.simple_spinner_item 中，
    主要的 TextView 的資源 ID 是 android.R.id.text1。這是最常用的選擇，適用於只顯示單行文字的情況。
    android.R.id.text2：在預設的佈局資源 android.R.layout.simple_list_item_2 中，
    次要的 TextView 的資源 ID 是 android.R.id.text2。如果您需要在項目中顯示主要和次要文字，則可以選擇這個資源 ID。
    ,
    陣列(資料集)
    )
    * */
}