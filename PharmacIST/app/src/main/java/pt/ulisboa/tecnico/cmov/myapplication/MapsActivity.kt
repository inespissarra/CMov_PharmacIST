package pt.ulisboa.tecnico.cmov.pharmacist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback{

    private val TAG = "MapsActivity"

    private lateinit var db: FirebaseFirestore

    private lateinit var mMap: GoogleMap
    private var center: LatLng? = null
    private lateinit var mapSearchView: SearchView
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private val markers = mutableListOf<Marker>()

    lateinit var pharmacyRepository: PharmacyRepository
    lateinit var favoritePharmaciesRepository: FavoritePharmaciesRepository
    lateinit var medicineWithNotificationRepository: MedicineWithNotificationRepository

    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

        createSearchBar()

        db = Firebase.firestore
        pharmacyRepository = PharmacyRepository(this)

        // notifications
        val serviceIntent = Intent(this, MedicineUpdateService::class.java)

        auth = Firebase.auth
        favoritePharmaciesRepository = FavoritePharmaciesRepository(this)
        medicineWithNotificationRepository = MedicineWithNotificationRepository(this)
        if (auth.currentUser != null) {
            getFavoritePharmacies()
            getMedicinesWithNotifications(object : getMedicinesWithNotificationsCallback{
                override fun onSuccess() {
                    startForegroundService(serviceIntent)
                }

                override fun onFailure() {}
            })
        }

        Log.d(TAG, "Finished onCreate")
    }


    override fun onResume() {
        super.onResume()
        createBottomNavigation()

        if (center!=null) {
            findNearbyPharmaciesFirebase(center!!.latitude, center!!.longitude, object : NearbyPharmaciesFirebaseCallback {
                override fun onSuccess() {
                    val pharmacies = pharmacyRepository.getNearbyPharmacies(center!!.latitude, center!!.longitude, 0.1)
                    markPlaces(pharmacies)
                }
                override fun onFailure() {}
            })
        }
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
        mapSearchView = findViewById(R.id.mapSearchView) as SearchView

        mapSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(name: String): Boolean {
                val pharmacyExists = getPharmacy(name)
                if (!pharmacyExists){
                    val geocoder = Geocoder(this@MapsActivity)
                    try {
                        val addressList = geocoder.getFromLocationName(name, 1)
                        if (!addressList.isNullOrEmpty()) {
                            val address = addressList[0]
                            val latLng = LatLng(address.latitude, address.longitude)
                            zoomOnMap(latLng)
                        } else {
                            showToast(R.string.no_pharmacies_and_places)
                        }
                    } catch (e : IOException){
                        Log.e(TAG, e.toString())
                    }


                }
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })
    }

    private fun getFavoritePharmacies(){
        favoritePharmaciesRepository.clearPharmacies()
        db.collection("users").document(auth.uid!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val favorites = document.get("favorite_pharmacies") as? List<String>
                    if (favorites != null) {
                        for (pharmacy in favorites) {
                            favoritePharmaciesRepository.insertOrUpdate(pharmacy)
                        }
                    }
                }
            }
    }

    private fun getMedicinesWithNotifications(callback: getMedicinesWithNotificationsCallback){
        medicineWithNotificationRepository.clearMedicines()
        db.collection("users").document(auth.uid!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val notifications = document.get("medicine_notifications") as? List<String>
                    if (notifications != null) {
                        for (medicine in notifications) {
                            medicineWithNotificationRepository.insertOrUpdate(medicine)
                        }
                    }
                }
            }
        callback.onSuccess()
    }

    interface getMedicinesWithNotificationsCallback {
        fun onSuccess()
        fun onFailure()
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
        //private fun checkIsFavorite() {
        //    db.collection("users").document(auth.uid!!).collection("favorite_pharmacies")
        //        .whereEqualTo("name", pharmacyName)
        //        .get()
        //        .addOnSuccessListener {documents ->
        //            isInUsersFavorite = !documents.isEmpty
        //            findViewById<ImageButton>(R.id.favoriteIcon).isSelected = isInUsersFavorite
        //        }
        //}
    }


    private fun getPharmacies() {
        center = mMap.cameraPosition.target
        var pharmacies = pharmacyRepository.getNearbyPharmacies(center!!.latitude, center!!.longitude, 0.1)
        if (pharmacies.isEmpty()) {
            findNearbyPharmaciesFirebase(center!!.latitude, center!!.longitude, object : NearbyPharmaciesFirebaseCallback {
                override fun onSuccess() {
                    pharmacies = pharmacyRepository.getNearbyPharmacies(center!!.latitude, center!!.longitude, 0.1)
                    markPlaces(pharmacies)
                }
                override fun onFailure() {
                    showToast(R.string.no_pharmacies_in_area)
                }
            })
        } else{
            markPlaces(pharmacies)
        }
    }

    private fun getPharmacy(name: String) : Boolean{
        val pharmacy : PharmacyMetaData? = pharmacyRepository.getPharmacy(name)
        var pharmacyExists = false
        if (pharmacy == null) {
            findPharmacyFirebase(name, object :PharmacyFirebaseCallback{
                override fun onSuccess(pharmacy: PharmacyMetaData) {
                    removeAllMarkers()
                    val location = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
                    addMarker(pharmacy.name!!, location)
                    zoomOnMap(location)
                    pharmacyExists = true
                }
                override fun onFailure() {}
            })
        } else {
            pharmacyExists = true
            removeAllMarkers()
            val location = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
            addMarker(pharmacy.name!!, location)
            zoomOnMap(location)
        }
        return pharmacyExists
    }

    private fun updatePharmaciesCache(pharmaciesList : ArrayList<PharmacyMetaData>){
        pharmacyRepository.clearPharmacies()
        for (pharmacy in pharmaciesList){
            pharmacyRepository.insertOrUpdate(pharmacy)
        }
    }

    private fun findPharmacyFirebase(name: String, callback: PharmacyFirebaseCallback){
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
            .addOnFailureListener {}
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
        val marker : Marker?
        if (favoritePharmaciesRepository.isFavoritePharmacy(name)){
            marker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title(name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.star_marker)))
        } else {
            marker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
            )
        }
        marker?.let { markers.add(it) }
    }

    private fun removeAllMarkers() {
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}