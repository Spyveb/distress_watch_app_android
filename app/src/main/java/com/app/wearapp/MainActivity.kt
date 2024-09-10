package com.app.wearapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.app.wearapp.ApiCall.RetrofitClient
import com.app.wearapp.UserAuth.Auth
import com.app.wearapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale


class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var binding: ActivityMainBinding
    private var messageEvent: MessageEvent? = null

    private val channelName = "watch_connectivity"

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var activityContext: Context? = null

    var latitude = 0.0
    var longitude = 0.0
    var randomString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        activityContext = this


        if (Auth.getToken(this) != null && Auth.getToken(this)?.isNotEmpty() == true) {

            checkToken()

            sOSButtonVisible()


            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                getCurrentLocation()

            } else {
                requestLocationPermissions()
            }

        } else {

            qrCodeDisplay()

        }


        binding.llSOS.setOnClickListener {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {

                Log.e("latitude", "" + latitude)
                Log.e("longitude", "" + longitude)
                val handler = Handler(Looper.getMainLooper())

                val dialog = Dialog(this, R.style.transparentDialog)
                dialog.setContentView(R.layout.dialog_sucess)
                val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)

                val dialogButton: Button = dialog.findViewById(R.id.btnDone)

                tvTitle.text = "You can undo this report within 5 seconds if it was sent by mistake."
                dialogButton.text = "Undo"

                val runnable = Runnable { // Code to execute after 5 seconds
                    dialog.dismiss()
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())

                    try {
                        val addresses: List<Address>? = geocoder.getFromLocation(
                            latitude,
                            longitude, 1
                        )
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val cityName = address.locality
                            val fullAddress = address.getAddressLine(0)

                            Log.e("LocationService", "City: $cityName")
                            Log.e("LocationService", "Address: $fullAddress")

                            sendData(address)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e("LocationService", "Geocoder failed")
                    }
                }
                dialogButton.setOnClickListener {
                    handler.removeCallbacks(runnable);

                    dialog.dismiss()
                }
                handler.postDelayed(runnable, 5000);

                dialog.show()

            } else {
                requestLocationPermissions()
            }
        }
    }

    private fun sOSButtonVisible() {

        binding.llSOS.isVisible = true
        binding.llMyQRButton.isVisible = false
        binding.llQR.isVisible = false

    }

    private fun pairButtonVisible() {

        binding.llSOS.isVisible = false
        binding.llQR.isVisible = false
        binding.llMyQRButton.isVisible = true
    }

    private fun qrCodeDisplay() {

        pairButtonVisible()

        randomString = getRandomString(32)


        // Generate QR code

        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix =
                multiFormatWriter.encode(randomString, BarcodeFormat.QR_CODE, 400, 400)
            val bitmap = bitMatrixToBitmapTransparent(bitMatrix)
            binding.imgQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.btnMyQR.setOnClickListener {
            binding.llQR.isVisible = true
            binding.llMyQRButton.isVisible = false
        }

        //(192*1.41-192)*1.41/4

        val width = Resources.getSystem().displayMetrics.widthPixels
        val height = Resources.getSystem().displayMetrics.heightPixels

        Log.e("width",""+width);
        Log.e("height",""+height);

        var value = (width*1.41-width)*1.41/4

        value += 10

        binding.imgQR.setPadding(value.toInt(), value.toInt(), value.toInt(), value.toInt())
    }

    fun sendData(location: Address) {

        val params: MutableMap<String, String> = HashMap()
        params["city"] = location.locality
        params["location"] = location.getAddressLine(0)
        params["latitude"] = location.latitude.toString()
        params["longitude"] = location.longitude.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.sendData(params)
                withContext(Dispatchers.Main) {


                    if (response.code() == 200){
                        val dialog = Dialog(this@MainActivity, R.style.transparentDialog)
                        dialog.setContentView(R.layout.dialog_sucess)
                        val dialogButton: Button = dialog.findViewById(R.id.btnDone)
                        val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
                        dialogButton.setOnClickListener {

                            dialog.dismiss()
                        }


                        if (response.body()?.success == true) {


                            tvTitle.text =
                                "Your SOS message has been sent. Help is on the way. Stay safe and keep your phone nearby."

                        } else {
                            tvTitle.text =
                                if (response.body()?.message != null) response.body()?.message else "Failed to send SOS emergency"

                        }
                        dialog.show()
                    }else
                        if (response.code() == 401) {


                            Auth.clearAllPreferences(this@MainActivity)

                            qrCodeDisplay()

                            val dialog = Dialog(this@MainActivity, R.style.transparentDialog)
                            dialog.setContentView(R.layout.dialog_sucess)
                            val dialogButton: Button = dialog.findViewById(R.id.btnDone)
                            val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
                            dialogButton.setOnClickListener {

                                dialog.dismiss()
                            }
                            dialogButton.text = "Ok"

                            tvTitle.text = "You are not authorized to access this application, Please reconnect again."

                            dialog.show()

                        }else{
                            val dialog = Dialog(this@MainActivity, R.style.transparentDialog)
                            dialog.setContentView(R.layout.dialog_sucess)
                            val dialogButton: Button = dialog.findViewById(R.id.btnDone)
                            val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
                            dialogButton.setOnClickListener {

                                dialog.dismiss()
                            }
                            dialogButton.text = "Ok"

                            tvTitle.text =
                                if (response.body()?.message != null) response.body()?.message else "Failed to send SOS emergency"

                            dialog.show()
                        }





                    Log.e("Results", "" + response.body()?.success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle any errors
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()

                    e.printStackTrace()
                }
            }
        }
    }


    fun checkToken() {


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.checkToken()
                withContext(Dispatchers.Main) {

                    if (response.body()?.success == true) {

                    } else {


                        val dialog = Dialog(this@MainActivity, R.style.transparentDialog)
                        dialog.setContentView(R.layout.dialog_sucess)
                        val dialogButton: Button = dialog.findViewById(R.id.btnDone)
                        val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
                        dialogButton.setOnClickListener {

                            dialog.dismiss()
                        }
                        dialogButton.text = "Ok"

                        tvTitle.text = "You are not authorized to access this application, Please reconnect again."

                        dialog.show()


                        Auth.clearAllPreferences(this@MainActivity)

                        qrCodeDisplay()


                    }


                    Log.e("Results", "" + response.body()?.success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle any errors
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()

                    e.printStackTrace()
                }
            }
        }
    }

    fun getRandomString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    private fun bitMatrixToBitmapTransparent(bitMatrix: BitMatrix): Bitmap {
        val trimmedMatrix = removeWhiteBorder(bitMatrix)
        val width = trimmedMatrix.width
        val height = trimmedMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (trimmedMatrix[x, y]) 0xFF000000.toInt() else 0x00000000
                ) // Black for QR code, transparent for background
            }
        }

        return bitmap
    }

    private fun removeWhiteBorder(matrix: BitMatrix): BitMatrix {
        var left = matrix.width
        var right = 0
        var top = matrix.height
        var bottom = 0

        // Find the active area (non-white)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix[x, y]) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        // Crop the bit matrix to remove the border
        val width = right - left + 1
        val height = bottom - top + 1
        val result = BitMatrix(width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (matrix[x + left, y + top]) {
                    result.set(x, y)
                }
            }
        }

        return result
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getMessageClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getMessageClient(activityContext!!).addListener(this)

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                // Handle the case where the user denied the permission
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    latitude = lat;
                    longitude = lon;

                } else {
                    // Retry with a new location request
                    requestNewLocationData()
                }
            }
            .addOnFailureListener {
                handleNullLocation() // Handle failure scenario
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude


                latitude = lat;
                longitude = lon;


            } else {
                handleNullLocation()
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    private fun handleNullLocation() {
        Toast.makeText(
            this,
            "Unable to retrieve location. Please check your settings.",
            Toast.LENGTH_LONG
        ).show()
    }


    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        try {
            val data = String(p0.data, StandardCharsets.UTF_8)

            val messageEventPath: String = p0.path

            val byteArrayInputStream = ByteArrayInputStream(p0.data)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            val hashMap = objectInputStream.readObject() as HashMap<String, Any>

            Log.e("hashMap", "" + hashMap.toString())

            messageEvent = p0


            if (hashMap["remove_device"] != null){

                Auth.clearAllPreferences(this@MainActivity)

                qrCodeDisplay()

                val dialog = Dialog(this@MainActivity, R.style.transparentDialog)
                dialog.setContentView(R.layout.dialog_sucess)
                val dialogButton: Button = dialog.findViewById(R.id.btnDone)
                val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
                dialogButton.setOnClickListener {

                    dialog.dismiss()
                }
                dialogButton.text = "Ok"

                tvTitle.text = "The device has been removed from the mobile app."

                dialog.show()
            }


            if (hashMap["qr_data"] != null) {

                val isValid = randomString.equals(hashMap["qr_data"])

                // Send msg

                sendMsg("qr_valid", isValid)


            } else if (hashMap["user_data"] != null) {
                val userData = hashMap["user_data"] as HashMap<String, Any>;
                for ((key, value) in userData) {

                    if (key.equals("user_id")) {
                        Auth.setId(this, value.toString())
                    }
                    if (key.equals("user_name")) {
                        Auth.setName(this, value.toString())
                    }
                    if (key.equals("user_token")) {
                        Auth.setToken(this, value.toString())
                    }

                }


                if (Auth.getToken(this) == null && Auth.getToken(this)?.isEmpty() == true) {
                    sendMsg("user_data_saved", false)
                } else {
                    sendMsg("user_data_saved", true)
                }


                if (messageEventPath.isNotEmpty() && messageEventPath == channelName) {

                    try {
                        sOSButtonVisible()

                        requestLocationPermissions()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                } else {

                    binding.llSOS.visibility = View.GONE

                }

            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendMsg(key: String, isValid: Boolean) {
        val messageData = objectToBytes(
            mapOf(
                key to isValid
            )
        )

        val nodeId: String = messageEvent?.sourceNodeId!!
        val sendMessageTask =
            Wearable.getMessageClient(activityContext!!)
                .sendMessage(nodeId, channelName, messageData)

        sendMessageTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.e("send1", "Message sent successfully")

            } else {
                Log.e("send1", "Message failed.")
            }
        }
    }

    private fun objectToBytes(`object`: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(`object`)
        return baos.toByteArray()
    }

}
