package pt.ulisboa.tecnico.cmov.pharmacist

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PharmacyInformationPanelActivity: AppCompatActivity() {

    companion object {
        val TAG = "PharmacyInformationPanelActivity"
    }

    private var db: FirebaseFirestore = Firebase.firestore
    private lateinit var pharmacy: PharmacyMetaData
    private var pharmacyName: String? = null
    private var pharmacyAddress: String? = null
    private var pharmacyLatLng: LatLng? = null
    private lateinit var medicineStock: MutableMap<MedicineMetaData, Int>
    private lateinit var ratingList: List<Int>
    private lateinit var auth: FirebaseAuth
    private var isInUsersFavorite = false
    private lateinit var adapter: ListMedicineAdapter
    private lateinit var stockListView: RecyclerView
    private var isLoading: Boolean = false
    private var hasMoreData: Boolean = true
    private var lastFetch: DocumentSnapshot? = null

    private lateinit var favoritePharmaciesRepository : FavoritePharmaciesRepository

    // notifications
    private var medicineUpdateService: MedicineUpdateService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MedicineUpdateService.LocalBinder
            medicineUpdateService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            medicineUpdateService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate initiated")

        favoritePharmaciesRepository = FavoritePharmaciesRepository(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information_panel)

        //createPharmacyName()
        getDataFromIntent()

        updateRatingList()
        ratingButtonEvent()

        createSharePharmacy()

        // createFavoriteStar()
        createGoToPharmacy()

        createManageStock()

        auth = Firebase.auth
        if (auth.currentUser != null) {
            Log.d(TAG, "current user not null")
            checkIsFavorite()
        }

        favoriteButtonEvent()

        // bind notifications service
        val intent = Intent(this, MedicineUpdateService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        Log.d(TAG, "onCreate finished")
    }

    override fun onResume() {
        super.onResume()
        createStockList()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun favoriteButtonEvent() {
        // handle click, add/remove favorite
        val favoriteButton : ImageButton = findViewById(R.id.favoriteIcon)
        favoriteButton.setOnClickListener {
            // only add if user is logged in
            if (auth.currentUser != null) {
                checkIsFavorite()
                if (isInUsersFavorite) {
                    removeFromFavorite()
                }
                else {
                    addToFavorite()
                }
            }
            else {
                showToast(R.string.not_logged_in)
            }
        }
    }

    private fun addToFavorite() {
        val updates = hashMapOf<String, Any>(
            "favorite_pharmacies" to FieldValue.arrayUnion(pharmacyName)
        )

        db.collection("users").document(auth.currentUser!!.uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                isInUsersFavorite = true
                findViewById<ImageButton>(R.id.favoriteIcon).isSelected = true
                Log.d(TAG, "addToFavorite: added to favorite")
                val favoritePharmaciesRepository = FavoritePharmaciesRepository(this)
                if (pharmacyName!=null) {
                    favoritePharmaciesRepository.insertOrUpdate(pharmacyName!!)
                    medicineUpdateService?.addNewPharmacyCollection(pharmacyName!!)
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "addToFavorite: failed to add to favorite due to ${e.message}")
                showToast(R.string.something_went_wrong)
            }

    }

    private fun removeFromFavorite() {
        val updatesToRemove = hashMapOf<String, Any>(
            "favorite_pharmacies" to FieldValue.arrayRemove(pharmacyName)
        )

        db.collection("users").document(auth.currentUser!!.uid)
            .update(updatesToRemove)
            .addOnSuccessListener {
                isInUsersFavorite = false
                findViewById<ImageButton>(R.id.favoriteIcon).isSelected = false
                Log.d(TAG, "removeFromFavorite: removed from favorite")
                if (pharmacyName!=null) {
                    favoritePharmaciesRepository.deletePharmacy(pharmacyName!!)
                    medicineUpdateService?.removePharmacyCollection(pharmacyName!!)
                }
            }
            .addOnFailureListener {
                Log.w(TAG, "removeFromFavorite: Failed to remove favorite")
                showToast(R.string.something_went_wrong)
            }
    }

    private fun checkIsFavorite() {
        isInUsersFavorite = favoritePharmaciesRepository.isFavoritePharmacy(pharmacyName!!)
        findViewById<ImageButton>(R.id.favoriteIcon).isSelected = isInUsersFavorite
    }

    private fun getDataFromIntent() {
        Log.d(TAG, "here")
        pharmacy = intent.getParcelableExtra<PharmacyMetaData>("pharmacy")!!
        val name : TextView = findViewById(R.id.pharmacyName)
        val address : TextView = findViewById(R.id.pharmacyLocation)
        val pharmacyImage: ImageView = findViewById(R.id.pharmacyImage)
        Log.d(TAG, "Got pharmacy $pharmacy")

        name.text = pharmacy.name
        pharmacyName = pharmacy.name
        address.text = pharmacy.locationName
        pharmacyAddress = pharmacy.locationName
        pharmacyLatLng = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
        Glide.with(this@PharmacyInformationPanelActivity).load(pharmacy.picture).into(pharmacyImage)
    }

    private fun createRatingHistogram() {
        val barChart = findViewById<BarChart>(R.id.barChart)

        // Calculate the frequency of each rating value
        val ratingCounts = IntArray(6)
        for (rating in ratingList) {
            ratingCounts[rating]++
        }

        Log.d(TAG, "rating counts: " + ratingCounts.joinToString(", "))

        // Prepare the data for the chart
        val entries = ArrayList<BarEntry>()
        for (i in 1..5) {
            entries.add(BarEntry(i.toFloat(), ratingCounts[i].toFloat()))
        }

        val barDataSet = BarDataSet(entries, "Ratings")
        barDataSet.color = ContextCompat.getColor(this, R.color.green_3)
        barDataSet.setDrawValues(false)

        barChart.data = BarData(barDataSet)
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawGridBackground(false)

        // Fetch the color from the attribute
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.axisLabelColor, typedValue, true)
        val axisLabelColor = typedValue.data

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.textColor = axisLabelColor
        xAxis.axisLineColor = axisLabelColor
        xAxis.axisLineWidth = 2f

        val maxValue = ratingCounts.maxOrNull() ?: 0

        // Set the axis minimum and maximum values
        val leftAxis: YAxis = barChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = (maxValue + 1).toFloat()

        if (maxValue < 10) {
            leftAxis.granularity = 1f
            leftAxis.isGranularityEnabled = true
        }
        leftAxis.setDrawGridLines(false)
        leftAxis.textColor = axisLabelColor
        leftAxis.axisLineColor = axisLabelColor
        leftAxis.axisLineWidth = 2f

        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.legend.textColor = axisLabelColor

        barChart.setExtraOffsets(0f, 0f, 0f, 20f)

        barChart.invalidate()

    }

    private fun ratingButtonEvent() {
        val rateButton: ImageButton = findViewById(R.id.rateButton)
        rateButton.setOnClickListener {
            if (auth.currentUser != null)
                showRatingDialog()
            else
                showToast(R.string.not_logged_in)
        }
    }

    private fun showRatingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.rating_dialog, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Rate the pharmacy")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val rating = mapOf("rate" to ratingBar.rating)
                Log.d(TAG, "Rating: $rating")

                // TODO: make this document an attribute of the class
                db.collection("pharmacies").document(pharmacyName!!).collection("ratings")
                    .document(auth.currentUser!!.uid)
                    .set(rating, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "rating added/updated successfully")
                        updateRatingList()
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "rating added/updated failed")
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun updateRatingList() {
        val list = mutableListOf<Int>()
        db.collection("pharmacies").document(pharmacyName!!).collection("ratings").get()
            .addOnSuccessListener { documents ->
                for (document in documents.documents) {
                    list.add(document.getLong("rate")!!.toInt())
                    Log.d(TAG, "updating list: " + list.joinToString(", "))
                }
                createRatingHistogram()
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to get ratings")
            }

        ratingList = list
    }

    private fun createSharePharmacy(){
        val sharePharmacy: ImageButton = findViewById(R.id.sharePharmacy)
        sharePharmacy.setOnClickListener {
            var shareMessage = getString(R.string.share_message_init)
            shareMessage += getString(R.string.share_message_name) + "$pharmacyName\n"
            shareMessage += getString(R.string.share_message_address) + "$pharmacyAddress\n"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareMessage)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun createGoToPharmacy() {
        val goToPharmacy: ImageView = findViewById(R.id.gotoPharmacy)
        goToPharmacy.setOnClickListener {
            val intent = Intent(this, DirectionsActivity::class.java)
            intent.putExtra("pharmacy", pharmacyName)
            intent.putExtra("address", pharmacyAddress)
            intent.putExtra("pharmacyLatitude", pharmacyLatLng!!.latitude)
            intent.putExtra("pharmacyLongitude", pharmacyLatLng!!.longitude)
            this.startActivity(intent)
        }
    }

    private fun createStockList() {
        stockListView = findViewById(R.id.stockList)
        stockListView.layoutManager = GridLayoutManager(this@PharmacyInformationPanelActivity, 1)
        medicineStock = mutableMapOf()
        adapter = ListMedicineAdapter(this, medicineStock, pharmacyName!!)
        stockListView.adapter = adapter

        stockListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading && hasMoreData) {
                    Log.d(TAG, "Loading more data, since we reached the bottom")
                    getStock()
                }
            }
        })

        adapter.onItemClick = {
            val intent = Intent(this, MedicineInformationPanelActivity::class.java)
            intent.putExtra("sender", "PharmacyInformationPanelActivity")
            intent.putExtra("medicine", it)
            Log.d(TAG, "Sending intent to MedicineInformationPanelActivity with medicine $it")
            startActivity(intent)
        }

        getStock()
    }

    private fun getStock() {
        isLoading = true

        Log.d(TAG, "Getting stock for pharmacy $pharmacyName from DB")
        db.collection("pharmacies")
            .whereEqualTo("name", pharmacyName)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.documents}")
                for (document in documents.documents) {

                    var medicineQuery = document.reference
                        .collection("medicines")
                        .limit(10)
                        .orderBy("name")
                        .whereGreaterThan("stock", 0)

                    lastFetch?.let {
                        medicineQuery = medicineQuery.startAfter(it)
                    }

                    medicineQuery.get().addOnSuccessListener {
                        for (doc in it.documents) {
                            Log.d(TAG, "Getting stock of medicine ${doc.id}")
                            val amount = doc.getLong("stock")?.toInt() ?: 0
                            if (amount != 0) {
                                val medicine = doc.toObject(MedicineMetaData::class.java)!!
                                medicineStock[medicine] = amount
                                adapter.addMedicineStock(medicine, amount)
                                adapter.notifyItemChanged(medicineStock.size - 1)
                            }
                        }
                        Log.d(TAG, "Final stock for $pharmacyName = $medicineStock")
                    }.addOnFailureListener {
                        Log.e(TAG, "Failed to get stock for pharmacy $pharmacyName, error=$it")
                        showToast(R.string.something_went_wrong)
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to get pharmacy $pharmacyName, error=$it")
                showToast(R.string.something_went_wrong)
            }
    }

    private fun createManageStock() {
        val manageStock: Button = findViewById(R.id.manageStock)
        manageStock.setOnClickListener {
            val intent = Intent(this, ScanBarcodeActivity::class.java)
            intent.putExtra("pharmacy", pharmacy)
            this.startActivity(intent)
        }
    }

    /**
     * Checks what type of connectivity the user has
     *
     * @param context The context of the caller
     * @return Int: 0 -> No connectivity; 1 -> Mobile Data; WiFi or stronger -> 2
     */
    private fun checkConnectivity(context: Context): Int {
        //
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return 1
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return 2
            }
        }
        return 0
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}