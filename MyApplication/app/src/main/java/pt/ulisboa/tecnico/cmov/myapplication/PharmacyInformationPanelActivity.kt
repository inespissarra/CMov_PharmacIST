package pt.ulisboa.tecnico.cmov.myapplication

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.SetOptions

class PharmacyInformationPanelActivity: AppCompatActivity() {

    companion object {
        val TAG = "PharmacyInformationPanelActivity"
    }

    private var db: FirebaseFirestore = Firebase.firestore
    private var pharmacyName: String? = null
    private var pharmacyAddress: String? = null
    private var pharmacyLatLng: LatLng? = null
    private lateinit var medicineStock: MutableMap<MedicineMetaData, Int>
    private lateinit var ratingList: List<Int>
    private lateinit var auth: FirebaseAuth
    private var isInUsersFavorite = false
    private lateinit var adapter: ListMedicineAdapter
    private lateinit var stockListView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate initiated")

        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information_panel)

        //createPharmacyName()
        getDataFromIntent()


        /*createPharmacyImage()
        createPharmacyAddress()*/
        updateRatingList()
        Log.d(TAG, "list after updated: " + ratingList.joinToString(", "))
        Log.d(TAG, "list only has zero: " + ratingList.all { it == 0 })
        /*if (! ratingList.all { it == 0 })
            createRatingHistogram()*/
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

        Log.d(TAG, "onCreate finished")
    }

    override fun onResume() {
        super.onResume()
        createStockList()
    }

    private fun favoriteButtonEvent() {
        // handle click, add/remove favorite
        val favoriteButton : ImageButton = findViewById(R.id.favoriteIcon)
        favoriteButton.setOnClickListener {
            // only add if user is logged in
            if (auth.currentUser != null && auth.uid != null) {
                checkIsFavorite()
                if (isInUsersFavorite) {
                    removeFromFavorite()
                }
                else {
                    addToFavorite()
                }
            }
            else {
                Toast.makeText(this, "You're not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToFavorite() {
        val data = hashMapOf("name" to pharmacyName)
        // TODO: make this document an attribute of the class
        db.collection("users").document(auth.uid!!).collection("favorite_pharmacies")
            .add(data)
            .addOnSuccessListener {
                isInUsersFavorite = true
                findViewById<ImageButton>(R.id.favoriteIcon).isSelected = true
                Log.d(TAG, "addToFavorite: added to favorite")
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "addToFavorite: failed to add to favorite due to ${e.message}")
                Toast.makeText(this, "Failed to add to favorite due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFromFavorite() {
        db.collection("users").document(auth.uid!!).collection("favorite_pharmacies")
            .whereEqualTo("name", pharmacyName)
            .get()
            .addOnSuccessListener {documents ->
                if (!documents.isEmpty) {
                    deleteDocument(documents.documents.first().id)
                    isInUsersFavorite = false
                    findViewById<ImageButton>(R.id.favoriteIcon).isSelected = false
                    Log.d(TAG, "removeFromFavorite: removed from favorite")
                }
                else {
                    Log.d(TAG, "removeFromFavorite: No pharmacy matched")
                    Toast.makeText(this, "No pharmacy matched", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {e ->
                Log.d(TAG, "removeFromFavorite: Failed to remove favorite")
                Toast.makeText(this, "Failed to remove favorite due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIsFavorite() {
        db.collection("users").document(auth.uid!!).collection("favorite_pharmacies")
            .whereEqualTo("name", pharmacyName)
            .get()
            .addOnSuccessListener {documents ->
                isInUsersFavorite = !documents.isEmpty
                findViewById<ImageButton>(R.id.favoriteIcon).isSelected = isInUsersFavorite
            }
    }

    private fun deleteDocument(documentId: String) {
        val docRef = db.collection("users").document(auth.uid!!).collection("favorite_pharmacies").document(documentId)

        docRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "Document with ID $documentId successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "Error deleting document: $e")
            }
    }

    private fun getDataFromIntent() {
        Log.d(TAG, "here")
        val pharmacy = intent.getParcelableExtra<PharmacyMetaData>("pharmacy")!!
        val name : TextView = findViewById(R.id.pharmacyName)
        val address : TextView = findViewById(R.id.pharmacyLocation)
        val pharmacyImage: ImageView = findViewById(R.id.pharmacyImage)

        name.text = pharmacy.name
        pharmacyName = pharmacy.name
        address.text = pharmacy.locationName
        pharmacyAddress = pharmacy.locationName
        pharmacyLatLng = LatLng(pharmacy.latitude!!, pharmacy.longitude!!)
        Log.d(TAG, pharmacyName.toString())
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

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        // xAxis.valueFormatter = IndexAxisValueFormatter(arrayListOf())

        val maxValue = ratingCounts.maxOrNull() ?: 0

        // Set the axis minimum and maximum values
        val leftAxis: YAxis = barChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = (maxValue + 1).toFloat()  // Adding 1 to leave some space at the top

        // Set the scale (granularity) based on the maximum value
        if (maxValue < 10) {
            leftAxis.granularity = 1f
            leftAxis.isGranularityEnabled = true
        }

        barChart.axisRight.isEnabled = false  // Disable right Y-axis

        barChart.axisLeft.setDrawGridLines(false)
        barChart.legend.isEnabled = true

        barChart.invalidate()

    }

    private fun ratingButtonEvent() {
        val rateButton: ImageButton = findViewById(R.id.rateButton)
        rateButton.setOnClickListener {
            if (auth.currentUser != null)
                showRatingDialog()
            else
            // TODO: display this error on the screen
                Log.e(TAG, "no user logged")
        }
    }

    private fun showRatingDialog() {
        // Inflate the dialog layout
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
        // TODO: maybe instead of accessing database, use a mutable map for ranking list (user -> rate), and thus update the map while update the rate
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
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT,
                    "Hey there! Check out the pharmacy I found!\n" +
                            "Name:$pharmacyName\n" +
                            "Address:$pharmacyAddress\n")
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
        adapter.onItemClick = {
            val intent = Intent(this, MedicineInformationPanelActivity::class.java)
            intent.putExtra("medicine", it)
            startActivity(intent)
        }

        getStock()
    }

    private fun getStock() {
        db.collection("stock")
            .whereEqualTo("pharmacy", pharmacyName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents.documents) {
                    val medicineName = document.getString("medicine")
                    val medicineImage = document.getString("image")
                    val medicineDescription = document.getString("description")
                    val amount = document.getLong("amount")!!.toInt()
                    if (amount != 0) {
                        val medicine = MedicineMetaData(name = medicineName, image = medicineImage,
                            description = medicineDescription)
                        medicineStock[medicine] = amount
                    }
                }
                adapter.setMedicineStockList(medicineStock)
                stockListView.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
            }
    }

    private fun createManageStock() {
        val manageStock: Button = findViewById(R.id.manageStock)
        manageStock.setOnClickListener {
            val intent = Intent(this, ScanBarcodeActivity::class.java)
            intent.putExtra("pharmacyName", pharmacyName)
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
}