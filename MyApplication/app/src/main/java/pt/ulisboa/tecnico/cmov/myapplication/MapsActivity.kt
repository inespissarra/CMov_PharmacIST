package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.SearchView
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
import com.google.android.gms.maps.model.Marker
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

    private val TAG = "MapsActivity"

    private lateinit var db: FirebaseFirestore

    private lateinit var mMap: GoogleMap
    private lateinit var autocompleteFragment:AutocompleteSupportFragment
    private lateinit var mapSearchView: SearchView
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markers = mutableListOf<Marker>()

    lateinit var pharmacyRepository: PharmacyRepository

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
        createSearchArea()
        createAddPharmacy()

        Places.initialize(applicationContext, getString(R.string.google_map_api_key))

        //createSearchBar()
        createSearchBar2()
        createBottomNavigation()

        pharmacyRepository = PharmacyRepository(this)

        Log.d(TAG, "Finished onCreate")
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

    private fun createSearchArea() {
        val searchAreaButton: Button = findViewById(R.id.searchArea)
        searchAreaButton.setOnClickListener{
            getPharmacies()
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
                    showToast("Some error in search") }
            }

            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng!!
                mMap.addMarker(MarkerOptions().position(latLng).title("$latLng"))
                zoomOnMap(latLng)
            }
        })
    }

    private fun createSearchBar2() {
        mapSearchView = findViewById(R.id.mapSearchView) as SearchView

        mapSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                getPharmacy(s)
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
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
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        recenterLocation()

        mMap.setOnMarkerClickListener { marker ->
            val pharmacyName = marker.title
            if (pharmacyName!=null){
                val pharmacy = pharmacyRepository.getPharmacy(pharmacyName)
                if (pharmacy != null) {
                    val intent =
                        Intent(applicationContext, PharmacyInformationPanelActivity::class.java)
                    intent.putExtra("pharmacy", pharmacy)
                    startActivity(intent)
                }
            }
            false
        }
    }

    private fun markPlaces(pharmaciesList: ArrayList<PharmacyMetaData>){
        removeAllMarkers()
        for(pharmacy in pharmaciesList){
            val latLng = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
            addMarker(pharmacy.name!!, latLng)
        }
        //for(latLng in staredPlaces){
        //    addStarMarker(latLng)
        //}
    }

    private fun getPharmacies() {
        val center = mMap.cameraPosition.target
        var pharmacies = pharmacyRepository.getNearbyPharmacies(center.latitude, center.longitude, 0.1)
        if (pharmacies.isEmpty()) {
            findNearbyPharmaciesFirebase(center.latitude, center.longitude, object : NearbyPharmaciesFirebaseCallback {
                override fun onSuccess() {
                    pharmacies = pharmacyRepository.getNearbyPharmacies(center.latitude, center.longitude, 0.1)
                    markPlaces(pharmacies)
                }
                override fun onFailure() {
                    showToast("There are no pharmacies in this area")
                }
            })
        } else{
            markPlaces(pharmacies)
        }
    }

    private fun getPharmacy(name: String) {
        val pharmacy : PharmacyMetaData? = pharmacyRepository.getPharmacy(name)
        if (pharmacy == null) {
            findPharmacyFirebase(name, object :PharmacyFirebaseCallback{
                override fun onSuccess(pharmacy: PharmacyMetaData) {
                    removeAllMarkers()
                    val location = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
                    addMarker(pharmacy.name!!, location)
                    zoomOnMap(location)
                }

                override fun onFailure() {
                    showToast("Pharmacy not found")
                }
            })
        } else {
            removeAllMarkers()
            val location = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
            addMarker(pharmacy.name!!, location)
            zoomOnMap(location)
        }
    }

    private fun updatePharmaciesCache(pharmaciesList : ArrayList<PharmacyMetaData>){
        pharmacyRepository.clearPharmacies()
        for (pharmacy in pharmaciesList){
            pharmacyRepository.insertOrUpdate(pharmacy)
        }
    }

    private fun findPharmacyFirebase(name: String, callback: PharmacyFirebaseCallback){
        db = Firebase.firestore
        val collectionRef = db.collection("pharmacies")
        var pharmacy : PharmacyMetaData? = null
        collectionRef.whereEqualTo("name", name).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    pharmacy = documents.documents.first().toObject(PharmacyMetaData::class.java)
                    if(pharmacy!=null) {
                        callback.onSuccess(pharmacy!!)
                    } else {
                        callback.onFailure()
                    }
                } else callback.onFailure()
            }
            .addOnFailureListener { exception ->
                showToast(exception.toString())
            }
        if (pharmacy!=null){
            findNearbyPharmaciesFirebase(pharmacy!!.latitude!!, pharmacy!!.longitude!!, object : NearbyPharmaciesFirebaseCallback {
                override fun onSuccess() {}
                override fun onFailure() {}
            })
        }
    }

    interface PharmacyFirebaseCallback {
        fun onSuccess(pharmacy : PharmacyMetaData)
        fun onFailure()
    }

    private fun findNearbyPharmaciesFirebase(latitude: Double, longitude: Double, callback: NearbyPharmaciesFirebaseCallback){
        val pharmaciesList = ArrayList<PharmacyMetaData>()
        db = Firebase.firestore
        val collectionRef =  db.collection("pharmacies")
        val query =  collectionRef.whereGreaterThan("latitude", latitude - 0.3)
            .whereLessThan("latitude", latitude + 0.3)
            .whereGreaterThan("longitude", longitude - 0.3)
            .whereLessThan("longitude", longitude + 0.3)
        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (data in documents.documents) {
                        val pharmacy: PharmacyMetaData? = data.toObject(PharmacyMetaData::class.java)
                        if (pharmacy != null) {
                            pharmaciesList.add(pharmacy)
                        }
                    }
                    updatePharmaciesCache(pharmaciesList)
                    callback.onSuccess()
                } else {
                    callback.onFailure()
                }
            }
            .addOnFailureListener { exception ->
                callback.onFailure()
                Log.d(TAG, exception.toString())
            }
    }

    interface NearbyPharmaciesFirebaseCallback {
        fun onSuccess()
        fun onFailure()
    }

    private fun addMarker(name:String, latLng: LatLng){
        val marker = mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title(name)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))
        marker?.let { markers.add(it) }
    }

    private fun addStarMarker(latLng: LatLng){
        val marker = mMap.addMarker(MarkerOptions()
            .position(latLng)
            .title("Star Marker")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.star_marker)))
        marker?.let { markers.add(it) }
    }

    private fun removeAllMarkers() {
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
    }

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}