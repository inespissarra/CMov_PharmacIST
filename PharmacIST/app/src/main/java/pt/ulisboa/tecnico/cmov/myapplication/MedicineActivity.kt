package pt.ulisboa.tecnico.cmov.pharmacist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class MedicineActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MedicineActivity"
        private const val LOCATION_REQUEST_CODE = 1
    }

    private var db: FirebaseFirestore = Firebase.firestore

    private lateinit var adapter: MedicineAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var medicineRepository: MedicineRepository
    private var indexMap: HashMap<String, Int> = HashMap()
    private var currentLocation: LatLng? = null
    private var medicineList: ArrayList<MedicinePharmacyDBEntryData> = ArrayList()
    private var filteredList: ArrayList<MedicinePharmacyDBEntryData> = ArrayList()
    private var lastFetch: DocumentSnapshot? = null
    private var isLoading: Boolean = false
    private var hasMoreData: Boolean = true
    private var inSearch: Boolean = false
    private var prevQuerySize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        setContentView(R.layout.activity_medicine)

        createBottomNavigation()

        medicineRepository = MedicineRepository(this)

        recyclerView = findViewById(R.id.medicineRecyclerView)
        searchView = findViewById(R.id.search_medicine)

        val gridLayoutManager = GridLayoutManager(this@MedicineActivity, 1)
        recyclerView.layoutManager = gridLayoutManager

        adapter = MedicineAdapter(this@MedicineActivity, filteredList)
        recyclerView.adapter = adapter

        adapter.onItemClick = {
            val intent = Intent(this, MedicineInformationPanelActivity::class.java)
            intent.putExtra("sender", "MedicineActivity")
            intent.putExtra("medicine", it)
            intent.putExtra("userLatitude", currentLocation?.latitude)
            intent.putExtra("userLongitude", currentLocation?.longitude)
            Log.d(TAG, "Sending intent $intent to MedicineInformationPanelActivity")
            startActivity(intent)
        }

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.length > prevQuerySize) {
                        // User added text
                        searchMedicine(newText, true)
                    } else if (it.length < prevQuerySize) {
                        // User deleted text
                        // This assumes the user typed the query one by one and thus
                        // substrings were already queried. If the user wrote it
                        // in one go, e.g. by pasting text, this will miss some results
                        searchMedicine(newText, false)
                    }
                    prevQuerySize = it.length
                }
                return true
            }

        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading && !inSearch && hasMoreData) {
                    Log.d(TAG, "Loading more data, since we reached the bottom")
                    loadData()
                }
            }
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getUserLocationAndLoadData()

        Log.d(TAG, "onCreate finished")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult - User granted permission")
                    loadData()
                }
                else showToast(R.string.error_verifying_location)
            }
        }
    }

    private fun checkLocationPermission() : Boolean {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestUserLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE)
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocationAndLoadData() {
        if (!checkLocationPermission()) {
            Log.d(TAG, "getUserLocationAndLoadData - Asking user for permission")
            requestUserLocation()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            currentLocation = LatLng(location.latitude, location.longitude)
            Log.d(TAG, "getUserLocationAndLoadData - Got location")
            loadData()
        }.addOnFailureListener {
            Log.d(TAG, "getUserLocationAndLoadData - Error getting location")
            loadData()
        }
    }

    private fun loadData() {
        Log.d(TAG, "Loading data from DB")
        isLoading = true

        var query = db.collection("medicines").limit(10).orderBy("name")

        lastFetch?.let {
            query = query.startAfter(it)
        }

        query.get().addOnSuccessListener { documents ->
            if (documents.size() < 10) hasMoreData = false

            Log.d(TAG, "loadData - Documents: ${documents.documents}")

            for (document in documents) {
                val medicineMetaData = document.toObject(MedicineMetaData::class.java)
                Log.d(TAG, "loadData - Registered MedicineMetaData = $medicineMetaData")
                val pharmaciesMap = hashMapOf<String, Pair<PharmacyMetaData, Int>>()
                val item = MedicinePharmacyDBEntryData(
                    medicineMetaData = medicineMetaData
                )

                if (medicineList.any { it.medicineMetaData == medicineMetaData }) continue

                Log.d(TAG, "loadData - New medicine, adding it")
                medicineList.add(item)
                filteredList.add(item)
                medicineRepository.insertOrUpdate(medicineMetaData)
                indexMap[medicineMetaData.name!!] = medicineList.size - 1

                val secondQuery = if (currentLocation == null) {
                    document.reference.collection("pharmacies")
                } else {
                    document.reference.collection("pharmacies")
                        .whereGreaterThan("stock", 0)
                        .whereGreaterThan("latitude", currentLocation!!.latitude - 0.3)
                        .whereLessThan("latitude", currentLocation!!.latitude + 0.3)
                        .whereGreaterThan("longitude", currentLocation!!.longitude - 0.3)
                        .whereLessThan("longitude", currentLocation!!.longitude + 0.3)
                }

                secondQuery.get().addOnSuccessListener { pharmacies ->
                    for (pharmacyDoc in pharmacies) {
                        val pharmacyData = pharmacyDoc.toObject(PharmacyMetaData::class.java)
                        val stock = pharmacyDoc.getLong("stock")?.toInt() ?: 0
                        pharmaciesMap[pharmacyData.name!!] = Pair(pharmacyData, stock)
                    }

                    Log.d(TAG, "loadData - Pharmacy map for medicine ${medicineMetaData.name} = $pharmaciesMap")

                    item.pharmacyMap = pharmaciesMap

                    Log.d(TAG, "loadData - Registered Item = $item")
                    // Get the closest pharmacy for each medicine
                    if (item.pharmacyMap != null && item.pharmacyMap!!.isNotEmpty() && currentLocation != null) {
                        Log.d(TAG, "loadData - Medicine ${medicineMetaData.name} has pharmacies & Location is $currentLocation")
                        val sortedPharmacies = item.pharmacyMap?.filterValues { it.second > 0 }?.values?.sortedBy {
                            it.first.getDistance(currentLocation!!.latitude, currentLocation!!.longitude)
                        }?.toCollection(ArrayList()) ?: ArrayList()
                        Log.d(TAG, "loadData - Pharmacies sorted = $sortedPharmacies")
                        item.closestPharmacy = sortedPharmacies[0].first
                        item.closestDistance = String.format(Locale.US, "%.1f", item.closestPharmacy!!
                            .getDistance(currentLocation!!.latitude, currentLocation!!.longitude)).toDouble()
                        Log.d(TAG, "loadData - Closest pharmacy to ${item.medicineMetaData?.name} is ${item.closestDistance} meters away")
                    }
                    adapter.notifyItemChanged(indexMap[item.medicineMetaData?.name]!!)
                }.addOnFailureListener {
                    Log.e(TAG, "loadData - Error loading pharmacies for medicine ${item.medicineMetaData?.name}, error=$it")
                }
            }
            if (!documents.isEmpty) {
                lastFetch = documents.documents[documents.size() - 1]
            }
            isLoading = false
            Log.d(TAG, "loadData - Finished loading data from DB, medicineList=$medicineList, filteredList=$filteredList")
        }.addOnFailureListener {
            showToast(R.string.error_loading_data)
        }
    }

    private fun searchMedicine(query: String?, new: Boolean) {
        filteredList.clear()
        if (query != null) {
            inSearch = true
            Log.d(TAG, "searchMedicine - Searching medicine for query=$query, new=$new")

            medicineList.filterTo(filteredList) {
                it.medicineMetaData?.name?.lowercase()?.contains(query.lowercase(Locale.getDefault())) == true
            }
            Log.d(TAG, "searchMedicine - Filtered list = $filteredList")

            if (query.length >= 2 && new && hasMoreData) {
                db.collection("medicines")
                    .orderBy("name")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {

                            val medicineMetaData = document.toObject(MedicineMetaData::class.java)
                            Log.d(TAG, "searchMedicine - Registered MedicineMetaData = $medicineMetaData")
                            val pharmaciesMap = hashMapOf<String, Pair<PharmacyMetaData, Int>>()

                            val item = MedicinePharmacyDBEntryData(
                                medicineMetaData = medicineMetaData
                            )

                            if (!filteredList.any { it.medicineMetaData == item.medicineMetaData }) {
                                filteredList.add(item)
                                Log.d(TAG, "searchMedicine - Added item $item to filteredList")
                                if (!medicineList.any { it.medicineMetaData == item.medicineMetaData }) {
                                    medicineList.add(item)
                                    indexMap[medicineMetaData.name!!] = medicineList.size - 1
                                    medicineRepository.insertOrUpdate(medicineMetaData)
                                    Log.d(TAG, "searchMedicine - Added item $item to medicineList")
                                }
                            } else continue

                            document.reference.collection("pharmacies").get()
                                .addOnSuccessListener { pharmacies ->
                                    for (pharmacyDoc in pharmacies) {
                                        val pharmacyData = pharmacyDoc.toObject(PharmacyMetaData::class.java)
                                        val stock = pharmacyDoc.getLong("stock")?.toInt() ?: 0
                                        pharmaciesMap[pharmacyData.name!!] = Pair(pharmacyData, stock)
                                    }
                                    Log.d(TAG, "searchMedicine - Pharmacy map for medicine ${medicineMetaData.name} = $pharmaciesMap")

                                    item.pharmacyMap = pharmaciesMap

                                    // Get the closest pharmacy for each medicine
                                    if (item.pharmacyMap!!.isNotEmpty() && currentLocation != null) {
                                        val sortedPharmacies = item.pharmacyMap?.filterValues { it.second > 0 }?.values?.sortedBy {
                                            it.first.getDistance(currentLocation!!.latitude, currentLocation!!.longitude)
                                        } as ArrayList<*>
                                        Log.d(TAG, "searchMedicine - Sorted pharmacies=$sortedPharmacies")
                                        item.closestPharmacy = sortedPharmacies[0] as PharmacyMetaData?
                                        item.closestDistance = item.closestPharmacy!!
                                            .getDistance(currentLocation!!.latitude, currentLocation!!.longitude)
                                    }
                                }
                        }
                        adapter.notifyDataSetChanged()
                    }.addOnFailureListener {
                        showToast(R.string.error_loading_data)
                    }
            }
            else {
                adapter.notifyDataSetChanged()
            }
            if (filteredList.isEmpty() && new) {
                showToast(R.string.no_medicine_found)
            }
        } else {
            Log.d(TAG, "searchMedicine - Empty query")
            inSearch = false
            filteredList.addAll(medicineList)
            adapter.notifyItemRangeChanged(0, filteredList.size)
        }
    }

    private fun createBottomNavigation() {
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavView.selectedItemId = R.id.nav_medicine
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicine -> true
                R.id.nav_map -> {
                    startActivity(Intent(applicationContext, MapsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(applicationContext, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}