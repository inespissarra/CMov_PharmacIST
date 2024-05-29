package pt.ulisboa.tecnico.cmov.myapplication

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DirectionsActivity : AppCompatActivity(), OnMapReadyCallback{

    private val TAG = "directionsActivity"

    private lateinit var mMap: GoogleMap
    private lateinit var currentLocation: Location
    private var pharmacyLocation: LatLng? = null
    private var pharmacyName: String? = null
    private var pharmacyAddress: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Starting onCreate")

        setContentView(R.layout.activity_directions)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createMapFragment()
        createBackButton()
        createDirectionsButton()

        Places.initialize(applicationContext, getString(R.string.google_map_api_key))

        Log.d(TAG, "Finished onCreate")
    }

    private fun createMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun createBackButton() {
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }
    }

    private fun createDirectionsButton() {
        val directionsButton: Button = findViewById(R.id.directionsButton)
        directionsButton.setOnClickListener{
            getCurrentLocation(object : LocationCallback {
                override fun onSuccess() {
                    directionsButton.visibility = View.INVISIBLE
                    requestDirections(pharmacyLocation!!)
                }
                override fun onFailure() {
                    showToast(R.string.error_verifying_location)
                }
            })
        }
    }

    interface LocationCallback {
        fun onSuccess()
        fun onFailure()
    }

    private fun getCurrentLocation(callback: LocationCallback) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                currentLocation = location
                val newLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                val latLngZoom = CameraUpdateFactory.newLatLngZoom(newLatLng, 18f)
                mMap.animateCamera(latLngZoom)
                callback.onSuccess()
            }
            else {
                callback.onFailure()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        pharmacyName = intent.getStringExtra("pharmacy")
        pharmacyAddress = intent.getStringExtra("address")
        val latitude = intent.getDoubleExtra("pharmacyLatitude", 0.0)
        val longitude = intent.getDoubleExtra("pharmacyLongitude", 0.0)
        pharmacyLocation = LatLng(latitude, longitude)

        addMarker(pharmacyName!!, pharmacyLocation!!)
        val latLngZoom = CameraUpdateFactory.newLatLngZoom(pharmacyLocation!!, 18f)
        mMap.animateCamera(latLngZoom)
    }

    private fun addMarker(name:String, latLng: LatLng){
        mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title(name)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))
    }


    private fun requestDirections(origin: LatLng) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        val destination = LatLng(currentLocation.latitude, currentLocation.longitude)
        val apiKey = getString(R.string.google_map_api_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        Log.d(TAG, "Making direction request to API")

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Request failed: $e")
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.body?.let {
                    val responseData = it.string()
                    Log.d(TAG, "Request response: $responseData")
                    val directionsResponse = Gson().fromJson(responseData, DirectionsResponse::class.java)
                    runOnUiThread {
                        drawPath(directionsResponse)
                    }
                }
            }
        })
    }

    private fun drawPath(directionsResponse: DirectionsResponse) {
        val polylineOptions = PolylineOptions()
            .clickable(true)
            .color(R.color.purple_500)
            .width(10f)

        if (directionsResponse.routes.isEmpty()) {
            Log.e(TAG, "No routes were found")
            return
        }
        val legs = directionsResponse.routes[0].legs[0]
        for (step in legs.steps) {
            val points = decodePoly(step.polyline.points)
            for (point in points) {
                polylineOptions.add(point)
            }
        }

        mMap.addPolyline(polylineOptions)
        Log.d(TAG, "Polyline added to map")
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng((lat / 1E5), (lng / 1E5))
            poly.add(p)
        }

        return poly
    }

    data class DirectionsResponse(
        val routes: List<Route>
    )

    data class Route(
        val legs: List<Leg>
    )

    data class Leg(
        val steps: List<Step>
    )

    data class Step(
        val polyline: Polyline
    )

    data class Polyline(
        val points: String
    )

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}