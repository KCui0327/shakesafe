package com.example.wander

import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_arduino_controller.*
//import kotlinx.android.synthetic.main.control_layout.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


class ArduinoControllerActivity: AppCompatActivity(){

    companion object{
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        lateinit var m_bluetoothManager: BluetoothManager
        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arduino_controller)
        m_address = intent.getStringExtra(ArduinoMainActivity.EXTRA_ADDRESS).toString()

        ConnectToDevice(this).execute()

        control_A.setOnClickListener{ sendCommand("1") } // apertando "a" no app, ativa o led no arduino
        control_B.setOnClickListener{ sendCommand("2") } // apertando "b" no app, ativa o led no arduino
        control_C.setOnClickListener{sendCommand("3")}
        control_D.setOnClickListener { sendCommand("4") }
        control_E.setOnClickListener { sendCommand("5") }
        control_F.setOnClickListener { sendCommand("6") }
        control_led_disconnect.setOnClickListener{ disconnect() }

    }

    private fun sendCommand(input: String){
        if(m_bluetoothSocket != null){
            try{
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun disconnect(){
        if(m_bluetoothSocket != null){
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>(){
        private var connectSucess: Boolean = true
        private val context: Context

        init{
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg p0: Void?): String {
            try{
                if(m_bluetoothSocket == null || !m_isConnected){
//                    val context = WeakReference(mContext).get()
//                    if (context == null) {
//                        return "None"
//                    }

                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//                    m_bluetoothManager = BluetoothManager.getA
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    if (ActivityCompat.checkSelfPermission(
                            this.context,
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
                        return "None"
                    }
                    println(m_myUUID)
                    m_bluetoothSocket = device.createRfcommSocketToServiceRecord(m_myUUID)
                    println("hkhjdfhjdhjdhj")
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    println(m_bluetoothSocket)
                    m_bluetoothSocket!!.connect()
                    println("hkhjdfhjdhjdhj")
                }
            }catch (e: IOException){
                connectSucess = false
                e.printStackTrace()
            }
            return null.toString()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectSucess){
                Log.i("data", "couldn't connect")
            }else{
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }
}