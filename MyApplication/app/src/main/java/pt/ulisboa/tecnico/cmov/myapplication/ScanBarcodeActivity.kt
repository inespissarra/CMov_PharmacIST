package pt.ulisboa.tecnico.cmov.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class ScanBarcodeActivity : AppCompatActivity() {

    companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101

        private const val TAG = "AddStockActivity"
    }

    private var imageUri: Uri?= null
    private lateinit var resultText: TextView
    private lateinit var addRegisterButton: Button
    private var pharmacyName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_barcode)
        Log.d(TAG, "onCreate started")

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

        Log.d(TAG, "onCreate finished")
    }

    private fun processImageForBarcode(imageUri: Uri?) {
        if (imageUri != null) {
            val image: Bitmap? = getBitmapFromURI(imageUri)
            if (image != null) {
                val inputImage = InputImage.fromBitmap(image, 0)
                val barcodeScanner = BarcodeScanning.getClient()

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isEmpty()){
                           showToast("Barcode detection failed. Please resubmit an image.")
                        } else processBarcode(barcodes.first().rawValue!!)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Barcode detection failed: $e")
                        showToast("Barcode detection failed. Please resubmit an image.")
                    }
                return
            }
        }
        showToast("Invalid image")
    }

    private fun processBarcode(barcode: String) {
        // TODO: Send barcode to db and verify existance of medicine
        if (true) {
            resultText.text = "Barcode: $barcode\nBarcode matches existant stock"
            addRegisterButton.text = "Add stock to medicine"
            addRegisterButton.setOnClickListener {
                val intent = Intent(this, AddStockActivity::class.java)
                intent.putExtra("pharmacyName", pharmacyName)
                intent.putExtra("medicineName", "Hey") // Add name of medicine
                this.startActivity(intent)
            }
        } else {
            resultText.text = "Barcode: $barcode\nBarcode doesn't match existant stock"
            addRegisterButton.text = "Register new medicine"
            addRegisterButton.setOnClickListener {
                val intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("pharmacyName", pharmacyName)
                intent.putExtra("medicineName", "Hey") // Add name of medicine
                intent.putExtra("barcode", barcode)
                this.startActivity(intent)
            }
        }
    }

    private fun getBitmapFromURI(uri: Uri): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri)
        return inputStream?.use { BitmapFactory.decodeStream(it) }
    }

    private fun pickImageCamera(){
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES)
        options.setPrompt("Scan a barcode")
        options.setCameraId(0) // Use a specific camera of the device

        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }

    // Register the launcher and result handler
    private val barcodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            processBarcode(result.contents)
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

            processImageForBarcode(imageUri)
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

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}