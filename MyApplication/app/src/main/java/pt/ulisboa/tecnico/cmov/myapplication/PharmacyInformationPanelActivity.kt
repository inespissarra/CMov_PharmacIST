package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PharmacyInformationPanelActivity: AppCompatActivity() {

    companion object {
        val TAG = "PharmacyInformationPanelActivity"
    }

    private var db: FirebaseFirestore = Firebase.firestore
    private var pharmacyLocation: String? = null
    private var pharmacyImageUrl: String? = null
    private var pharmacyName: String? = null
    private var pharmacyAddress: String? = null
    private var isFavorite: Boolean = false
    private var medicineStock: ArrayList<MedicineMetaData> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate initiated")

        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information_panel)

        createPharmacyName()

        queryDB()

        createPharmacyImage()
        createPharmacyAddress()
        createRatingBar()

        createFavoriteStar()
        createGoToPharmacy()

        createStockList()
        createManageStock()

        Log.d(TAG, "onCreate finished")
    }

    private fun createPharmacyName() {
        val pharmacyNameTextView: TextView = findViewById(R.id.pharmacyName)
        val intentPharmacyName = intent.getCharSequenceExtra("pharmacyName")
        if (intentPharmacyName == null) pharmacyNameTextView.text = "ErrorName"
        else {
            pharmacyNameTextView.text = intentPharmacyName
            pharmacyName = intentPharmacyName.toString()
        }
        if (intentPharmacyName == "ErrorName") Log.e(TAG, "Error loading pharmacy's name")
    }

    private fun createPharmacyAddress() {
        val pharmacyLocationTextView: TextView = findViewById(R.id.pharmacyLocation)
        var intentPharmacyLocation = intent.getCharSequenceExtra("address")
        if (intentPharmacyLocation == null) {
            if (pharmacyLocation == null) Log.e(TAG, "Error loading pharmacy's location")
            intentPharmacyLocation = pharmacyLocation
        }
        pharmacyLocationTextView.text = intentPharmacyLocation ?: "ErrorLocation"
        pharmacyAddress = intentPharmacyLocation.toString()
    }

    private fun createPharmacyImage() {
        val image: ImageView = findViewById(R.id.pharmacyImage)
        Glide.with(this).load(pharmacyImageUrl).into(image)
    }

    private fun queryDB() {
        val connectivityFlag = checkConnectivity(this)
        db.collection("pharmacies").get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    for (document in it.documents) {
                        val name = document.get("name")
                        // TODO: Get stock 😔
                        if (name != null && name == pharmacyName) {
                            pharmacyLocation = document.get("locationName") as String?
                            pharmacyImageUrl = document.get("image") as String?
                        }
                    }
                }
            }
    }

    private fun createRatingBar() {
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        // TODO: Part 2 - Update with average of ratings
        ratingBar.rating = 3.5F
    }

    private fun createFavoriteStar() {
        val favoriteStar: ImageView = findViewById(R.id.favoriteStar)
        favoriteStar.setOnClickListener {
            isFavorite = !isFavorite
            // TODO: Update database
            if (isFavorite)
                favoriteStar.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.favorite_star_on)
                )
            else
                favoriteStar.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.favorite_star_off)
                )
        }
    }

    private fun createGoToPharmacy() {
        val goToPharmacy: ImageView = findViewById(R.id.gotoPharmacy)
        goToPharmacy.setOnClickListener {
            // TODO: Might require changes
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("pharmacy", pharmacyName)
            intent.putExtra("address", pharmacyAddress)
            this.startActivity(intent)
        }
    }

    private fun createStockList() {
        val stockList: RecyclerView = findViewById(R.id.stockList)
        stockList.adapter = ListMedicineAdapter(this, medicineStock)
    }

    private fun createManageStock() {
        val manageStock: Button = findViewById(R.id.manageStock)
        manageStock.setOnClickListener {
            val intent = Intent(this, ScanBarcodeActivity::class.java)
            intent.putExtra("name", pharmacyName)
            intent.putExtra("address", pharmacyAddress)
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