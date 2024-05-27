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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.maps.model.LatLng

class PharmacyInformationPanelActivity: AppCompatActivity() {

    companion object {
        val TAG = "PharmacyInformationPanelActivity"
    }

    private var db: FirebaseFirestore = Firebase.firestore
    private var pharmacyName: String? = null
    private var pharmacyAddress: String? = null
    private lateinit var pharmacy: PharmacyMetaData
    private var pharmacyLatLng: LatLng? = null
    private lateinit var medicineStock: MutableMap<MedicineMetaData, Int>
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
        createRatingBar()

        // createFavoriteStar()
        createGoToPharmacy()

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

    override fun onResume() {
        super.onResume()
        createStockList()
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

    private fun createRatingBar() {
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        // TODO: Part 2 - Update with average of ratings
        ratingBar.rating = 3.5F
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