package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.Status
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
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback{

    private val TAG = "MapsActivity"

    private lateinit var db: FirebaseFirestore

    private lateinit var mMap: GoogleMap
    private lateinit var autocompleteFragment:AutocompleteSupportFragment
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pharmaciesList: ArrayList<PharmacyMetaData>

    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Starting onCreate")

        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createMapFragment()
        createRecenterLocation()
        createAddPharmacy()

        Places.initialize(applicationContext, getString(R.string.google_map_api_key))

        createSearchBar()
        createBottomNavigation()

        Log.d(TAG, "Finished onCreate")
    }

    /*
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Starting onResume")
        createBottomNavigation()
        updatePharmacies()
        Log.d(TAG, "Finished onResume")
    }
    */

    private fun updatePharmacies() {
        pharmaciesList = ArrayList()
        eventChangeListener() // TODO fazer algo mais eficiente
    }

    private fun createMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun createRecenterLocation() {
        val recenterLocationButton: ImageButton = findViewById(R.id.recenterButton)
        recenterLocationButton.setOnClickListener{
            recenterLocation()
        }
    }

    private fun createAddPharmacy(){
        val addPharmacyButton: ImageButton = findViewById(R.id.addPharmacyButton)
        addPharmacyButton.setOnClickListener{
            startActivity(Intent(applicationContext, AddPharmacyActivity::class.java))
        }
    }

    private fun createSearchBar() {
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onError(p0: Status) {
                if (!p0.isCanceled) {
                    Toast.makeText(this@MapsActivity, "Some error in search", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng!!
                zoomOnMap(latLng)
            }
        })
    }

    private fun createBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_map
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_map -> true
                R.id.nav_medicine -> {
                    startActivity(Intent(applicationContext, MedicineActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(applicationContext, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun recenterLocation() {
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
                val currentLatLng = LatLng(location.latitude, location.longitude)
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f)
                mMap.animateCamera(newLatLngZoom)
            }
        }
    }

    private fun zoomOnMap(latLng: LatLng) {
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
        mMap.animateCamera(newLatLngZoom)
        mMap.addMarker(MarkerOptions().position(latLng).title("$latLng"))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        recenterLocation()

        mMap.setOnMarkerClickListener { marker ->
            val pharmacyName = marker.title
            val pharmacy = pharmaciesList.find { it.name == pharmacyName }
            val intent = Intent(applicationContext, PharmacyInformationPanelActivity::class.java)
            intent.putExtra("pharmacy", pharmacy)
            startActivity(intent)
            false
        }

        pharmaciesList = ArrayList()
        eventChangeListener()
    }

    private fun markPlaces(){
        for(pharmacy in pharmaciesList){
            val latLng = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
            addMarker(pharmacy.name!!, latLng)
        }
        //for(latLng in staredPlaces){
        //    addStarMarker(latLng)
        //}
    }

    private fun eventChangeListener() {
        db = Firebase.firestore
        db.collection("pharmacies").get().
            addOnSuccessListener {
            if (!it.isEmpty) {
                for (data in it.documents) {
                    val pharmacy: PharmacyMetaData? = data.toObject(PharmacyMetaData::class.java)
                    if (pharmacy != null) {
                        pharmaciesList.add(pharmacy)
                    }
                }
                markPlaces()
                val pharmacyName = intent.getStringExtra("pharmacyName")
                // Intent from PharmacyInformationPanelActivity to draw directions
                handleDirection(pharmacyName)
            }
        }.
        addOnFailureListener {
            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun addMarker(name:String, latLng: LatLng){
        mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title(name)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))

    }

    private fun addStarMarker(latLng: LatLng){
        mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title("Star Marker")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.star_marker)))
    }

    private fun handleDirection(pharmacyName: String?) {
        if (pharmacyName != null) {
            Log.d(TAG, "Detected intent for directions, getting location of ${pharmacyName}...")
            val pharmacy = pharmaciesList.find { it.name == pharmacyName }
            if (pharmacy != null) {
                if (pharmacy.latitude != null && pharmacy.longitude != null) {
                    requestDirections(
                        LatLng(pharmacy.latitude!!, pharmacy.longitude!!),
                        LatLng(currentLocation.latitude, currentLocation.longitude)
                    )
                } else {
                    if (pharmacy.latitude == null) {
                        Log.e(TAG, "Pharmacy has no latitude}")
                        Toast.makeText(this, "Pharmacy has no latitude", Toast.LENGTH_SHORT).show()
                    }
                    if (pharmacy.longitude == null) {
                        Log.e(TAG, "Pharmacy has no longitude")
                        Toast.makeText(this, "Pharmacy has no longitude", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else {
                Log.e(TAG, "Pharmacy not found")
                Toast.makeText(this, "Pharmacy not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestDirections(origin: LatLng, destination: LatLng) {
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
}