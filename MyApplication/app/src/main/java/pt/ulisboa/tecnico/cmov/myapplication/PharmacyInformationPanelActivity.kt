package pt.ulisboa.tecnico.cmov.myapplication

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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PharmacyInformationPanelActivity: AppCompatActivity() {

    companion object {
        val TAG = "PharmacyInformationPanelActivity"
    }

    private lateinit var pharmacy: PharmacyMetaData

    private var db: FirebaseFirestore = Firebase.firestore
    private var pharmacyLocation: String? = null
    private var pharmacyImageUrl: String? = null
    private var pharmacyName: String? = null
    private var pharmacyAddress: String? = null
    private var pharmacyLatLng: LatLng? = null
    private var isFavorite: Boolean = false
    private var medicineStock: ArrayList<MedicineMetaData> = ArrayList()
    private lateinit var auth: FirebaseAuth
    private var isInUsersFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate initiated")

        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information_panel)

        //createPharmacyName()
        getDataFromIntent()

        queryDB()

        /*createPharmacyImage()
        createPharmacyAddress()*/
        createRatingBar()

        // createFavoriteStar()
        createGoToPharmacy()

        createStockList()
        createManageStock()

        auth = Firebase.auth
        if (auth.currentUser != null) {
            Log.d(TAG, "current user not null")
            checkIsFavorite()
        }

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

        Log.d(TAG, "onCreate finished")
    }

    private fun addToFavorite() {
        val data = hashMapOf("name" to pharmacyName)
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
        pharmacy = intent.getParcelableExtra<PharmacyMetaData>("pharmacy")!!
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
        val favoriteStar: ImageView = findViewById(R.id.favoriteIcon)
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
            val intent = Intent(this, DirectionsActivity::class.java)
            intent.putExtra("pharmacyName", pharmacyName)
            intent.putExtra("pharmacyAddress", pharmacyAddress)
            intent.putExtra("pharmacyLatitude", pharmacyLatLng!!.latitude)
            intent.putExtra("pharmacyLongitude", pharmacyLatLng!!.longitude)
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