package pt.ulisboa.tecnico.cmov.pharmacist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MedicineInformationPanelActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MedicineInformationPanelActivity"
        private const val LOCATION_REQUEST_CODE = 1
    }

    private var db: FirebaseFirestore = Firebase.firestore

    private lateinit var adapter: ListPharmacyAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var medicineName: String
    private lateinit var medicineNameView: TextView
    private lateinit var medicineImageView: ImageView
    private lateinit var medicineDescriptionView: TextView
    private var isInUsersNotifications = false
    private lateinit var notificationButton : ImageButton
    private var auth: FirebaseAuth = Firebase.auth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var pharmacyList: ArrayList<Pair<PharmacyMetaData, Pair<Double, Int>>> = ArrayList()
    private var medicineEntry: MedicinePharmacyDBEntryData? = null
    private var medicine: MedicineMetaData? = null
    private var originalDescription: String? = null
    private var translatedDescription: String? = null
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private lateinit var targetLanguage: String
    private var isShowingTranslatedText: Boolean = false

    private lateinit var medicineWithNotificationRepository : MedicineWithNotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_information_panel)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.pharmacyList)

        val gridLayoutManager = GridLayoutManager(this@MedicineInformationPanelActivity, 1)
        recyclerView.layoutManager = gridLayoutManager

        adapter = ListPharmacyAdapter(this, pharmacyList)
        recyclerView.adapter = adapter
        adapter.setPharmacyDataList(pharmacyList)

        targetLanguage = Locale.getDefault().language

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val intentSender = intent.getStringExtra("sender")
        Log.d(TAG, "Received intent from $intentSender")
        if (intentSender == "MedicineActivity") {
            getMedicineEntryFromIntent()
            setupAdapter()
        } else if (intentSender == "PharmacyInformationPanelActivity") {
            getMedicineMetaDataFromIntent()
            getUserLocationAndLoadData()
        }

        medicineWithNotificationRepository = MedicineWithNotificationRepository(this)
        if (auth.currentUser != null) {
            checkHasNotification()
        }
        notificationButton = findViewById(R.id.notificationButton)
        notificationButton.setOnClickListener {
            if (auth.currentUser != null) {
                if (isInUsersNotifications) {
                    removeFromNotifications()
                }
                else {
                    addToNotifications()
                }
            }
            else {
                showToast(R.string.not_logged_in)
            }
        }

        adapter.onItemClick = {
            val intent = Intent(this, PharmacyInformationPanelActivity::class.java)
            intent.putExtra("pharmacy", it)
            Log.d(TAG, "Sending intent with $it to PharmacyInformationPanelActivity")
            startActivity(intent)
        }
    }

    private fun addToNotifications() {
        val updates = hashMapOf<String, Any>(
            "medicine_notifications" to FieldValue.arrayUnion(medicineName)
        )

        db.collection("users").document(auth.currentUser!!.uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                isInUsersNotifications = true
                findViewById<ImageButton>(R.id.notificationButton).isSelected = true
                Log.d(PharmacyInformationPanelActivity.TAG, "addToNotifications: added to notifications")
                medicineWithNotificationRepository.insertOrUpdate(medicineName)
            }
            .addOnFailureListener { e ->
                Log.d(PharmacyInformationPanelActivity.TAG, "addToNotifications: failed to add to notifications due to ${e.message}")
                showToast(R.string.something_went_wrong)
            }

    }

    private fun removeFromNotifications() {
        val updatesToRemove = hashMapOf<String, Any>(
            "medicine_notifications" to FieldValue.arrayRemove(medicineName)
        )

        db.collection("users").document(auth.currentUser!!.uid)
            .update(updatesToRemove)
            .addOnSuccessListener {
                isInUsersNotifications = false
                findViewById<ImageButton>(R.id.notificationButton).isSelected = false
                Log.d(TAG, "removeFromNotifications: removed from notifications")
                    medicineWithNotificationRepository.deleteMedicine(medicineName)
            }
            .addOnFailureListener {
                Log.w(TAG, "removeFromNotifications: Failed to remove notifications")
                showToast(R.string.something_went_wrong)
            }
    }


    private fun checkHasNotification() {
        isInUsersNotifications = medicineWithNotificationRepository.isMedicineWithNotification(medicineName)
        findViewById<ImageButton>(R.id.notificationButton).isSelected = isInUsersNotifications
    }

    private fun checkLocationPermission() : Boolean {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestUserLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocationAndLoadData() {
        if (!checkLocationPermission()) {
            Log.d(TAG, "getUserLocationAndLoadData - Asking user for permission")
            requestUserLocation()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            userLatitude = location.latitude
            userLongitude = location.longitude
            Log.d(TAG, "getUserLocationAndLoadData - Got location")
            loadData()
        }.addOnFailureListener {
            Log.d(TAG, "getUserLocationAndLoadData - Error getting location")
            loadData()
        }
    }

    private fun setIntentValues(medicineMetaData: MedicineMetaData?) {
        if (medicineMetaData == null) return
        medicineNameView = findViewById(R.id.medicineName)
        medicineImageView = findViewById(R.id.medicineImage)
        medicineDescriptionView = findViewById(R.id.medicineDescription)

        if (medicineMetaData.name != null) {
            medicineName = medicineMetaData.name!!
            medicineNameView.text = medicineMetaData.name
        } else {
            medicineNameView.text = this.getString(R.string.name_not_found)
        }
        Log.d(TAG, "Got medicine name: ${medicineNameView.text}")

        Glide.with(this@MedicineInformationPanelActivity).load(medicineMetaData.image).into(medicineImageView)

        if (medicineMetaData.description != null) {
            originalDescription = medicineMetaData.description
            if (targetLanguage == "en") {
                medicineDescriptionView.text = medicineMetaData.description
            }
            else{
                translateText(originalDescription!!, targetLanguage)
            }

        } else {
            medicineDescriptionView.text = this.getString(R.string.description_not_found)
        }
    }

    private fun toggleTranslation() {
        val toggleTranslationView = findViewById<TextView>(R.id.toggle_translation)
        toggleTranslationView.visibility = View.VISIBLE
        toggleTranslationView.isClickable = true
        toggleTranslationView.setOnClickListener {
            if (isShowingTranslatedText) {
                medicineDescriptionView.text = originalDescription
                toggleTranslationView.text = getString(R.string.see_translated_text)
            }
            else {
                medicineDescriptionView.text = translatedDescription
                toggleTranslationView.text = getString(R.string.see_original_text)
            }
            isShowingTranslatedText = !isShowingTranslatedText
        }
    }

    private fun translateText(text: String, targetLanguage: String) {

        // Create an options object for the translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH) // Source language: English
            .setTargetLanguage(targetLanguage) // Target language
            .build()

        // Get a translator instance
        val translator: Translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        medicineDescriptionView.text = translatedText
                        translatedDescription = translatedText
                        isShowingTranslatedText = true
                        toggleTranslation()
                    }
                    .addOnFailureListener {
                        showToast(R.string.something_went_wrong)
                    }
            }
            .addOnFailureListener {
                Log.w(TAG, "failed to download model")
            }
    }

    @Suppress("DEPRECATION")
    private fun getMedicineMetaDataFromIntent() {
        medicine = intent.getParcelableExtra("medicine")

        Log.d(TAG, "Retrieved medicine: $medicine")
        if (medicine != null) {
            setIntentValues(medicine)
        }
    }

    @Suppress("DEPRECATION")
    private fun getMedicineEntryFromIntent() {
        medicineEntry = intent.getParcelableExtra("medicine")
        userLatitude = intent.getDoubleExtra("userLatitude", 0.0)
        userLongitude = intent.getDoubleExtra("userLongitude", 0.0)

        Log.d(TAG, "Retrieved medicine entry: $medicineEntry")
        Log.d(TAG, "User location: ($userLatitude, $userLongitude)")

        if (medicineEntry != null) {
            setIntentValues(medicineEntry?.medicineMetaData)
        }
    }

    private fun loadData() {
        if (medicine == null) {
            Log.e(TAG, "loadData - Medicine is null")
            if (medicineEntry != null) {
                Log.d(TAG, "loadData - Medicine Entry is not null")
                setupAdapter()
            }
            return
        }
        Log.d(TAG, "Loading data from DB")

        // TODO: Make location be null or something else if the user didn't give permission
        val query: Query = if (userLatitude == 0.0 && userLongitude == 0.0) {
            db.collection("medicines")
                .document(medicineName)
                .collection("pharmacies")
        } else {
            db.collection("medicines")
                .document(medicineName)
                .collection("pharmacies")
                .whereGreaterThan("stock", 0)
                .whereGreaterThan("latitude", userLatitude - 0.3)
                .whereLessThan("latitude", userLatitude + 0.3)
                .whereGreaterThan("longitude", userLongitude - 0.3)
                .whereLessThan("longitude", userLongitude + 0.3)
        }

        Log.d(TAG, "Starting query")
        query.get().addOnSuccessListener { documents ->

            Log.d(TAG, "Documents: ${documents.documents}")

            for (document in documents) {
                Log.d(TAG, "Found document ${document.data}")
                val pharmacyMetaData = document.toObject(PharmacyMetaData::class.java)
                Log.d(TAG, "Registered PharmacyMetaData = $pharmacyMetaData")
                val stock = document.getLong("stock")?.toInt() ?: 0

                pharmacyList.add(Pair(pharmacyMetaData,
                    Pair(pharmacyMetaData.getDistance(userLatitude, userLongitude), stock)))
            }
            Log.d(TAG, "Finished loading data from DB, pharmacyList=$pharmacyList")
            adapter.notifyItemRangeChanged(0, pharmacyList.size)
        }.addOnFailureListener {
            Log.e(TAG, "Error loading data from DB, error=$it")
            Toast.makeText(this, R.string.error_loading_data, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAdapter() {
        if (medicineEntry == null) {
            Log.e(TAG, "setupAdapter - Medicine entry is null")
            if (medicine != null) {
                Log.d(TAG, "setupAdapter - Medicine is not null")
                loadData()
            }
            return
        }
        val pharmacyMap = medicineEntry?.pharmacyMap

        if (pharmacyMap != null) {
            for ((_, pharmacyPair) in pharmacyMap) {
                val pharmacyMetaData = pharmacyPair.first
                val stock = pharmacyPair.second
                val distance = pharmacyMetaData.getDistance(userLatitude, userLongitude)
                Log.d(TAG, "Adding ${Pair(pharmacyMetaData, Pair(distance, stock))} to pharmacy list")
                pharmacyList.add(Pair(pharmacyMetaData, Pair(distance, stock)))
            }
            val sortedList = pharmacyList.sortedBy { it.second.first }
            pharmacyList.clear()
            pharmacyList.addAll(sortedList)
            Log.d(TAG, "Final pharmacy list: $pharmacyList")
            Log.d(TAG, "Finished adapter")
        } else {
            Log.d(TAG, "Pharmacy map is null")
            medicine = medicineEntry?.medicineMetaData
            loadData()
        }
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}