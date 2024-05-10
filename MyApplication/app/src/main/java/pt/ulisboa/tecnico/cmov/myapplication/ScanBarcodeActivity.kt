package pt.ulisboa.tecnico.cmov.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class ScanBarcodeActivity : AppCompatActivity() {

    companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101

        private const val TAG = "AddStockActivity"
    }

    private var imageUri: Uri?= null
    private lateinit var imageIv: ImageView
    private lateinit var resultText: TextView
    private lateinit var addRegisterButton: Button
    private var pharmacyName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_barcode)
        Log.d(TAG, "onCreate started")

        imageIv = findViewById(R.id.submittedImage)

        createBottomNavigation()

        val scanBtn = findViewById<MaterialButton>(R.id.scanBtn)
        scanBtn.setOnClickListener {
            if (checkCameraPermissions()) pickImageCamera()
            else requestCameraPermissions()
        }

        val galleryButton: MaterialButton = findViewById(R.id.galleryBtn)
        galleryButton.setOnClickListener {
            if (checkStoragePermissions()) pickImageGallery()
            else requestStoragePermissions()
        }

        resultText = findViewById(R.id.resultIv)
        addRegisterButton = findViewById(R.id.addRegisterButton)

        pharmacyName = intent.getStringExtra("pharmacyName") ?: "ErrorName"

        val submitButton: Button = findViewById(R.id.submitButton)
        submitButton.setOnClickListener {
            if (imageUri != null) {
                val bitmap: Bitmap? = getBitmapFromURI(imageUri!!)
                if (bitmap != null) processImageForBarcode(bitmap)
            }
        }

        Log.d(TAG, "onCreate finished")
    }

    private fun processImageForBarcode(image: Bitmap) {
        val inputImage = InputImage.fromBitmap(image, 0)
        val barcodeScanner = BarcodeScanning.getClient()

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                processBarcodes(barcodes)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode detection failed: $e")
                Toast.makeText(this, "Barcode detection failed. Please " +
                        "resubmit an image.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processBarcodes(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            val rawValue = barcode.rawValue
            val valueType = barcode.valueType

            Log.d(TAG, "Found barcode: Raw value: $rawValue, Value type: $valueType")

            val intent: Intent
            // TODO: Send barcode to db and verify existance of medicine
            if (true) {
                resultText.text = "Barcode matches existant stock"
                addRegisterButton.text = "Add stock to medicine"
                intent = Intent(this, AddStockActivity::class.java)
                intent.putExtra("medicineName", "Hey") // Add name of medicine
            } else {
                resultText.text = "Barcode doesn't match existant stock"
                addRegisterButton.text = "Register new medicine"
                intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("barcode", rawValue)
            }
            intent.putExtra("pharmacyName", pharmacyName)
            this.startActivity(intent)
        }
    }

    private fun getBitmapFromURI(uri: Uri): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri)
        return inputStream?.use { BitmapFactory.decodeStream(it) }
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

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "cameraActivityResult: imageUri: $imageUri")
            imageIv.setImageURI(imageUri)

        }
        else {
            Toast.makeText(this, "Canceled...!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
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

    private fun checkCameraPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE)
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermissions() {
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    pickImageCamera()
                else showToast("Camera permissions are required")
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    pickImageGallery()
                else showToast("Storage permissions are required")
            }
        }
    }

    private fun createBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.invisible
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_map -> {
                    startActivity(Intent(applicationContext, MapsActivity::class.java))
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