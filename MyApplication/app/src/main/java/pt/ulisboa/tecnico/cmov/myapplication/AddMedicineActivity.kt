package pt.ulisboa.tecnico.cmov.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddMedicineActivity : AppCompatActivity() {

    companion object {
        const val TAG = "AddMedicineActivity"

        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    private lateinit var imageIv: ImageView
    private lateinit var nameText: EditText
    private lateinit var amountText: EditText
    private lateinit var purposeText: EditText
    private var barcode: String? = null
    private var pharmacy: String? = null
    private var imageUri: Uri? = null
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate Started")

        setContentView(R.layout.activity_add_medicine)

        createBottomNavigation()

        imageIv = findViewById(R.id.photo)

        val cameraButton: ImageButton = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener{
            if (checkCameraPermissions() || requestCameraPermissions()) pickImageCamera()
        }

        val photoField: TextView = findViewById(R.id.photoField)
        photoField.setOnClickListener {
            if (checkCameraPermissions() || requestCameraPermissions()) pickImageCamera()
        }

        val galleryButton: ImageButton = findViewById(R.id.galleryButton)
        galleryButton.setOnClickListener{
            if (checkStoragePermissions() || requestStoragePermissions()) pickImageGallery()
        }

        nameText = findViewById(R.id.nameField)
        amountText = findViewById(R.id.amountField)
        purposeText = findViewById(R.id.purposeField)
        barcode = intent.getStringExtra("barcode")
        pharmacy = intent.getStringExtra("pharmacyName")
        db = Firebase.firestore

        val registerButton: Button = findViewById(R.id.registerBtn)
        registerButton.setOnClickListener{
            registerMedicine()
        }
    }

    private fun registerMedicine() {
        if (nameText.text.toString().takeIf { it.isNotBlank() } != null &&
            amountText.text.toString().takeIf { it.isNotBlank() } != null &&
            purposeText.text.toString().takeIf { it.isNotBlank() } != null) {
            val medicineName = nameText.text.toString()
            val purpose = purposeText.text.toString()
            val amount = amountText.text.toString().toInt()
            addNewMedicine(medicineName, purpose)
            addNewMedicineToStock(medicineName, purpose, amount)
        }
        else
            showToast("Must fill all mandatory fields")
    }

    private fun addNewMedicine(medicineName: String, purpose: String) {
        val medicine = hashMapOf(
            "name" to medicineName,
            "description" to purpose,
            "image" to imageUri
        )
        db.collection("medicines").document(medicineName)
            .set(medicine)
            .addOnSuccessListener {
                Log.d(TAG, "Medicine registered successfully")
                showToast("Medicine registered successfully")
                finish()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error registering medicine")
                showToast("Error registering medicine")
            }
    }

    private fun addNewMedicineToStock(medicineName: String, purpose: String, amount: Int) {
        val stock = hashMapOf(
            "medicine" to medicineName,
            "description" to purpose,
            "image" to imageUri,
            "pharmacy" to pharmacy,
            "amount" to amount
        )
        db.collection("stock").document(pharmacy + "_" + medicineName)
            .set(stock)
            .addOnSuccessListener {
                Log.d(TAG, "Medicine registered successfully")
                showToast("Medicine registered successfully")
                finish()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error registering medicine")
                showToast("Error registering medicine")
            }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private fun pickImageCamera(){
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

            imageUri = data?.data

            Log.d(TAG, ": imageUri: $imageUri")
            imageIv.setImageURI(imageUri)
        }
        else {
            Toast.makeText(this, "Canceled...!", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data

            imageUri = data?.data
            Log.d(TAG, "cameraActivityResult: imageUri: $imageUri")
            imageIv.setImageURI(imageUri)
        }
        else{
            Toast.makeText(this, "Canceled...!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermissions(): Boolean {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
        return checkCameraPermissions()
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                STORAGE_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_REQUEST_CODE
            )
        }
        return checkStoragePermissions()
    }

    private fun createBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.invisible
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_map -> {
                    //startActivity(Intent(applicationContext, MapsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_medicine -> {
                    startActivity(Intent(applicationContext, MedicineActivity::class.java))
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

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}