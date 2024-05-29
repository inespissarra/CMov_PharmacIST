package pt.ulisboa.tecnico.cmov.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
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
import java.util.Locale

@Suppress("DEPRECATION")
class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var autocompleteFragment:AutocompleteSupportFragment
    private lateinit var selectedLocation: LatLng
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
                val intent = Intent()
                intent.putExtra("latitude", selectedLocation.latitude)
                intent.putExtra("longitude", selectedLocation.longitude)
                val geoCoder = Geocoder(this, Locale.getDefault())
                val addresses = geoCoder.getFromLocation(selectedLocation.latitude, selectedLocation.longitude, 1)
                if (addresses != null) {
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val addressFragments = address.getAddressLine(0)
                        intent.putExtra("name", addressFragments)
                    }
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else{
                showToast(R.string.select_location)
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
            recenterLocation(true)
        }
    }

    private fun createSearchBar() {
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onError(p0: Status) {
                showToast(R.string.something_went_wrong)
            }

            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng!!
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                mMap.animateCamera(newLatLngZoom)
                addMarker(latLng)
            }
        })
    }

    private fun recenterLocation(marker : Boolean) {
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
                selectedLocation = LatLng(location.latitude, location.longitude)
                if (marker){
                    addMarker(selectedLocation)
                }
                else{
                    val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(selectedLocation, 18f)
                    mMap.animateCamera(newLatLngZoom)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        mMap.setOnMapLongClickListener { position -> addMarker(position) }
        val lat = intent.getDoubleExtra("lat", Double.MIN_VALUE)
        val lng = intent.getDoubleExtra("lng", Double.MIN_VALUE)
        if (lat != Double.MIN_VALUE && lng != Double.MIN_VALUE){
            addMarker(LatLng(lat, lng))

        } else{
            recenterLocation(false)
        }
    }

    private fun addMarker(latLng: LatLng){
        if(selected!=0){
            marker.remove()
        } else{
            selected = 1
        }
        selectedLocation = latLng
        marker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Marker")
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))!!
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(selectedLocation, 18f)
        mMap.animateCamera(newLatLngZoom)
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}