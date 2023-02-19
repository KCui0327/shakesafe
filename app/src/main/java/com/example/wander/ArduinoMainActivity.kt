package com.example.wander

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.*
import kotlinx.android.synthetic.main.activity_arduino_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast


class ArduinoMainActivity : AppCompatActivity() {

    var m_bluetoothAdapter: BluetoothAdapter? = null
    lateinit var m_pairedDevices: Set<BluetoothDevice>
    val REQUEST_ENABLE_BLUETOOTH = 1

    companion object{
        val EXTRA_ADDRESS: String = "Device_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arduino_main)

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(m_bluetoothAdapter == null){
            toast("this device doesn't support bluetooth")
            return
        }
        if(!m_bluetoothAdapter!!.isEnabled){
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        select_device_refresh.setOnClickListener{ pairedDeviceList() }

    }

    private fun pairedDeviceList(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list:ArrayList<BluetoothDevice> = ArrayList()

        if(!m_pairedDevices.isEmpty()){
            for(device: BluetoothDevice in m_pairedDevices){
                list.add(device)
                Log.i("device", ""+device)
            }
        }else{
            toast("no paired bluetooth devices found")
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        select_device_list.adapter = adapter
        select_device_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = list[position]
            val address: String = device.address

            val intent = Intent(this, ArduinoControllerActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BLUETOOTH){
            if(resultCode == Activity.RESULT_OK){
                if(m_bluetoothAdapter!!.isEnabled){
                    toast("Bluetooth has been enabled.")
                }else{
                    toast("Bluetooth has been disabled.")
                }
            }else if(resultCode == Activity.RESULT_CANCELED){
                toast("Bluetooth enabling has been canceled.")
            }
        }
    }
}