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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MapsActivity : AppCompatActivity(), OnMapReadyCallback{

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
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createMapFragment()
        createRecenterLocation()
        createAddPharmacy()

        Places.initialize(applicationContext, getString(R.string.google_map_api_key))

        createSearchBar()
        createBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        createBottomNavigation()
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

        pharmaciesList = ArrayList()
        eventChangeListener()
    }

    private fun markPlaces(){
        //val myPlaces = listOf(
        //    LatLng(38.736946, -9.142685),
        //    LatLng( 37.426, -122.163),
        //    LatLng(37.430, -122.173),
        //    LatLng(37.444, -122.170)
        //)
        //val staredPlaces = listOf(
        //    LatLng(38.6642, -9.07666),
        //    LatLng(1.282, 103.864),
        //    LatLng( 1.319, 103.706),
        //    LatLng( 1.249, 103.830),
        //    LatLng( 1.3138, 103.8159)
        //)

        for(pharmacy in pharmaciesList){
            var latLng = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
            addMarker(latLng)
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
            }
        }.
        addOnFailureListener {
            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun addMarker(latLng: LatLng){
        mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title("Marker")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))
    }

    private fun addStarMarker(latLng: LatLng){
        mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title("Star Marker")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.star_marker)))
    }
}