package pt.ulisboa.tecnico.cmov.myapplication

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var autocompleteFragment:AutocompleteSupportFragment
    private lateinit var selectedLocation: Location
    private lateinit var marker: Marker
    private var selected: Int = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_location)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createMapFragment()
        createRecenterLocation()

        Places.initialize(applicationContext, getString(R.string.google_map_api_key))

        createSearchBar()

        val cancelButton: Button = findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener{
            finish()
        }

        val selectButton: Button = findViewById(R.id.selectButton)
        selectButton.setOnClickListener{
            if(selected!=0){
                finish()
            } else{
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
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
            addMarker(LatLng(selectedLocation.latitude, selectedLocation.longitude))
        }
    }

    private fun createSearchBar() {
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onError(p0: Status) {
                Toast.makeText(this@SelectLocationActivity, "Some error in search", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng!!
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                mMap.animateCamera(newLatLngZoom)
                addMarker(latLng)
            }
        })
    }

    private fun recenterLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                selectedLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f)
                mMap.animateCamera(newLatLngZoom)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        mMap.setOnMapLongClickListener { position -> addMarker(position) }

        recenterLocation()
    }

    private fun addMarker(latLng: LatLng){
        if(selected!=0){
            marker.remove()
        } else{
            selected = 1
        }
        marker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Marker")
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))!!
    }
}