/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wander


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.text.SimpleDateFormat



//This class allows you to interact with the map by adding markers, styling its appearance and
// displaying the user's location.
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val age: Int,
    val urgency: Int,
    val nearbyPeople: Int,
    val notes: String?
)

val locations = listOf(
    LocationData(38.7083, 39.2559, "Ayşe Güneş", 32, 9, 2, "Trapped in collapsed apartment building"),
    LocationData(38.7219, 39.2816, "Mehmet Yılmaz", 45, 10, 1, null),
    LocationData(38.7241, 39.2832, "Emre Yıldız", 22, 8, 3, "Under a collapsed roof"),
    LocationData(38.7053, 39.2244, "Cemal Demir", 60, 7, 4, "Trapped in basement of collapsed building"),
    LocationData(38.7100, 39.2320, "Hüseyin Öztürk", 28, 5, 1, null)
)
data class VictimData(
    val name: String,
    val urgency: Int,
    val numPeople: Int,
    val latitude: Double,
    val longitude: Double
)
data class Message(val sender: String, val body: String, val timestamp: Long)

//interface DropboxApi {
//    @POST("/upload")
//    fun uploadImage(@Body image: RequestBody): Call<>
//}

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val TAG = MapsActivity::class.java.simpleName
    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    val victims = mutableListOf<VictimData>()
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near the Googleplex.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        //These coordinates represent the lattitude and longitude of the Googleplex.
        val latitude = 41.0082
        val longitude = 28.9784
        val zoomLevel = 15f
        val overlaySize = 100f
        val homeLatLng = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        map.addMarker(MarkerOptions().position(homeLatLng))

        val googleOverlay = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.android))
            .position(homeLatLng, overlaySize)
        map.addGroundOverlay(googleOverlay)

        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        checkLocationPermission()
        showLocationsOnMap()
        addButton()
        addChatButton()
        addArduinoButton()
        addCameraButton()
    }

    private fun dispatchTakePictureIntent() {
        println("fuck")

        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            println("fuckity")
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            println("fuck fuck")
            uploadImageToDropbox(imageBitmap)
        }
    }

    private fun uploadImageToDropbox(imageBitmap: Bitmap) {
        Thread(Runnable {
            val dbxClientV2 = DbxClientV2(DbxRequestConfig.newBuilder("dropbox/java-tutorial").build(), "sl.BZHEtbzjBFLGtHksZMHotjP8tCAZ5dlEksjuqGjrmDogAM2iU6s0Ne5tDVA3B1DEmjlU70fBxbT7AutoX_1e1JZKel8jbgsMVMH1NdRGUkfyN8sEStNkXc6_s_u2K7YRCudcwFIiLnon")
            val outputStream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val uploadBuilder = dbxClientV2.files().uploadBuilder("/" + SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(Date()) +".jpg")
            uploadBuilder.uploadAndFinish(inputStream)
        }).start()
    }

    private fun addCameraButton(){
        val addCameraButton = findViewById<FloatingActionButton>(R.id.camera_button)
        addCameraButton.setOnClickListener(View.OnClickListener { //Start your second activity
            dispatchTakePictureIntent()
        })

    }

    private fun addArduinoButton(){
        val addArduinotButton = findViewById<FloatingActionButton>(R.id.arduino_button)
        addArduinotButton.setOnClickListener(View.OnClickListener { //Start your second activity
            val intent = Intent(this, ArduinoMainActivity::class.java)
            startActivity(intent)
        })
    }


    private fun addChatButton() {
        val addChatButton = findViewById<FloatingActionButton>(R.id.chat_button)
        addChatButton.setOnClickListener(View.OnClickListener { //Start your second activity
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        })
    }

    private fun showChatView() {
        println("lol")
    }

    private fun addButton() {
        val addButton = findViewById<FloatingActionButton>(R.id.add_location_button)
        addButton.setOnClickListener {
            showLocationForm()
        }
    }
    private fun showLocationForm() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.location_form, null)
        bottomSheet.setContentView(view)

        val nameField = view.findViewById<EditText>(R.id.name_field)
        val urgencyField = view.findViewById<EditText>(R.id.urgency_field)
        val peopleNearbyField = view.findViewById<EditText>(R.id.people_nearby_field)
        val submitButton = view.findViewById<Button>(R.id.submit_button)
        submitButton.isEnabled = false
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val name = nameField.text.toString()
                val urgency = urgencyField.text.toString()
                val peopleNearby = peopleNearbyField.text.toString()
                submitButton.isEnabled = name.isNotEmpty() && urgency.isNotEmpty() && peopleNearby.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        nameField.addTextChangedListener(textWatcher)
        urgencyField.addTextChangedListener(textWatcher)
        peopleNearbyField.addTextChangedListener(textWatcher)

        submitButton.setOnClickListener {
            val name = nameField.text.toString()
            val urgency = urgencyField.text.toString().toInt()
            val peopleNearby = peopleNearbyField.text.toString().toInt()

            try{
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val MainLocation = LocationData(location.latitude, location.longitude, name, 32, urgency, peopleNearby, "biacth")
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        val markerOptions = MarkerOptions()
                            .position(LatLng(MainLocation.latitude, MainLocation.longitude))
                            .title(MainLocation.name)
                            .snippet("Urgency: ${MainLocation.urgency}, Nearby People: ${MainLocation.nearbyPeople}")
                        map.addMarker(markerOptions)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }
                }
            } catch (e: SecurityException) {
                println("oooooo")
                Log.e("Exception: %s", e.message, e)
            }
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }


    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
//            showUserLocationOnMap()
            println("ayee")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                showUserLocationOnMap()
                println("woah")
            }
        }
    }

    private fun showUserLocationOnMap() {
        // Get the user's location
        println("over here bitch")
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val MainLocation = LocationData(location.latitude, location.longitude, "BD380", 32, 9, 2, "Trapped in collapsed apartment building")
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    val markerOptions = MarkerOptions()
                        .position(LatLng(MainLocation.latitude, MainLocation.longitude))
                        .title(MainLocation.name)
                        .snippet("Urgency: ${MainLocation.urgency}, Nearby People: ${MainLocation.nearbyPeople}")
                    map.addMarker(markerOptions)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        } catch (e: SecurityException) {
            println("oooooo")
            Log.e("Exception: %s", e.message, e)
        }


    }

    private fun showLocationsOnMap() {
        for (location in locations) {
            val markerOptions = MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(location.name)
                .snippet("Urgency: ${location.urgency}, Nearby People: ${location.nearbyPeople}")
            map.addMarker(markerOptions)
        }
        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
    }

    // Initializes contents of Activity's standard options menu. Only called the first time options
    // menu is displayed.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_options, menu)
        return true
    }

    // Called whenever an item in your options menu is selected.
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // Called when user makes a long press gesture on the map.
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }
    }

    // Places a marker on the map and displays an info window that contains POI name.
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
        }
    }

    // Allows map styling and theming to be customized.
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    // Checks that users have given permission
    private fun isPermissionGranted() : Boolean {
       return ContextCompat.checkSelfPermission(
            this,
           Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Checks if users have given their location and sets location enabled if so.
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
        }
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

}
