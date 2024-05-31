package pt.ulisboa.tecnico.cmov.pharmacist

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class ScanBarcodeActivity : AppCompatActivity() {

    companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101

        private const val TAG = "ScanBarcodeActivity"
    }

    private var imageUri: Uri?= null
    private lateinit var resultText: TextView
    private lateinit var addRegisterButton: Button
    private var pharmacyName: String? = null
    private lateinit var pharmacy: PharmacyMetaData
    private var db: FirebaseFirestore = Firebase.firestore

    @Suppress("DEPRECATION")
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

        pharmacy = intent.getParcelableExtra("pharmacy")!!
        pharmacyName = pharmacy.name

        resultText = findViewById(R.id.resultIv)
        addRegisterButton = findViewById(R.id.addRegisterButton)

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
                           showToast(R.string.barcode_detection_failed)
                        } else processBarcode(barcodes.first().rawValue!!)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Barcode detection failed: $e")
                        showToast(R.string.barcode_detection_failed)
                    }
                return
            }
        }
        showToast(R.string.invalid_image)
    }

    private fun processBarcode(barcode: String) {
        var medicine: MedicineMetaData
        db.collection("medicines")
            .whereEqualTo("barcode", barcode)
            .get()
            .addOnSuccessListener {documents ->
                if (!documents.isEmpty) {
                    // if there is a medicine with such barcode
                    medicine = documents.documents[0].toObject(MedicineMetaData::class.java)!!
                    resultText.text = getString(R.string.barcode_matches_existent_stock, barcode)
                    addRegisterButton.text = getString(R.string.add_stock_to_medicine)
                    addRegisterButton.setOnClickListener {
                        addAmountDialog(medicine, object: AddAmountCallback{
                            override fun onSuccess(){}
                            override fun onFailure(){}
                        })
                    }
                }
                else {
                    resultText.text =
                        getString(R.string.barcode_doesnt_match_existent_stock, barcode)
                    addRegisterButton.text = getString(R.string.register_new_medicine)
                    addRegisterButton.setOnClickListener {
                        val intent = Intent(this, AddMedicineActivity::class.java)
                        intent.putExtra("pharmacy", pharmacy)
                        intent.putExtra("barcode", barcode)
                        this.startActivity(intent)
                        finish()
                    }
                }
            }
            .addOnFailureListener {
                showToast(R.string.something_went_wrong)
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
    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            showToast(R.string.canceled)
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
            showToast(R.string.canceled)
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
                else showToast(R.string.camera_permissions)
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    pickImageGallery()
                else showToast(R.string.storage_permissions)
            }
        }
    }

    private fun addAmountDialog(medicine: MedicineMetaData, callback: AddAmountCallback) {
        var amount : Int?
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.amount_dialog_box_layout, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.amount_editText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("For medicine ${medicine.name}")
            .setView(dialogLayout)
            .setPositiveButton("OK") { _, _ ->
                amount = editText.text.toString().toIntOrNull()
                if (amount == null) {
                    Log.e(TAG, "User tried to input a non-number")
                    showToast(R.string.input_must_be_number)
                    return@setPositiveButton
                } else if (amount == 0) {
                    Log.e(TAG, "User tried to input 0")
                    showToast(R.string.input_must_not_be_zero)
                    return@setPositiveButton
                }
                addStock(medicine, amount!!)
                callback.onSuccess()
            }
            . setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "Cancel button clicked")
                dialog.dismiss()
                callback.onFailure()
            }
            .create()

        dialog.show()
    }
    interface AddAmountCallback {
        fun onSuccess()
        fun onFailure()
    }

    private fun addStock(medicine: MedicineMetaData, amount: Int) {
        Log.d(TAG, "pharmacy name: " + pharmacyName + "\nmedicine name: " + medicine.name)

        val stockRef = db
            .collection("pharmacies")
            .document(pharmacyName!!)
            .collection("medicines")
            .document(medicine.name!!)
        val stockRef2 = db
            .collection("medicines")
            .document(medicine.name!!)
            .collection("pharmacies")
            .document(pharmacyName!!)

        db.runTransaction { transaction ->
            val document = transaction.get(stockRef)
            val document2 = transaction.get(stockRef2)

            if (document.exists() && document2.exists()) {
                Log.d(TAG, "Both documents exist")
                val stock = document.getLong("stock")?.toInt() ?: 0
                val newStock = stock + amount

                if (newStock < 0) {
                    Log.e(TAG, "New stock would be negative")
                    throw Exception("Stock cannot be negative!")
                } else if (newStock == 0) {
                    stockRef.delete().addOnSuccessListener {
                        Log.d(TAG, "Stock became 0 and was removed from pharmacies")
                        return@addOnSuccessListener
                    }.addOnFailureListener {
                        Log.e(BuyMedicineActivity.TAG, "Error removing stock from pharmacies")
                        throw Exception("Error removing stock from pharmacies")
                    }
                    stockRef2.delete().addOnSuccessListener {
                        Log.d(TAG, "Stock became 0 and was removed from medicines")
                        return@addOnSuccessListener
                    }.addOnFailureListener {
                        Log.e(BuyMedicineActivity.TAG, "Error removing stock from medicines")
                        throw Exception("Error removing stock from medicines")
                    }
                } else {
                    Log.d(TAG, "newStock: $newStock in both collections")
                    transaction.update(stockRef, "stock", newStock)
                    transaction.update(stockRef2, "stock", newStock)
                }
            } else if (document.exists()) {
                Log.e(TAG, "Document does not exist in medicines")

                val stock = document.getLong("stock")?.toInt() ?: 0
                val newStock = stock + amount

                if (newStock < 0) {
                    Log.e(TAG, "New stock would be negative")
                    throw Exception("Stock cannot be negative!")
                }

                if (amount <= 0) {
                    Log.e(TAG, "Amount is negative or zero and stock is 0")
                    throw Exception("Amount is negative or zero and stock is 0")
                }
                val add = hashMapOf(
                    "name" to pharmacy.name,
                    "locationName" to pharmacy.locationName,
                    "longitude" to pharmacy.longitude,
                    "latitude" to pharmacy.latitude,
                    "picture" to pharmacy.picture,
                    "stock" to newStock
                )
                stockRef2.set(add)
                    .addOnSuccessListener {
                        Log.d(TAG, "Stock created on medicines successfully")
                    }.addOnFailureListener {
                        Log.e(TAG, "Error creating stock on medicines, e=$it")
                        throw Exception("Error creating stock on medicines")
                    }
                transaction.update(stockRef, "stock", newStock)
            } else if (document2.exists()) {
                val stock = document.getLong("stock")?.toInt() ?: 0
                val newStock = stock + amount

                if (newStock < 0) {
                    Log.e(TAG, "New stock would be negative")
                    throw Exception("Stock cannot be negative!")
                }

                Log.e(TAG, "Document does not exist in pharmacies")
                if (amount <= 0) {
                    Log.e(TAG, "Amount is negative or zero and stock is 0")
                    throw Exception("Amount is negative or zero and stock is 0")
                }
                val add = hashMapOf(
                    "name" to medicine.name,
                    "description" to medicine.description,
                    "image" to medicine.image,
                    "stock" to newStock
                )
                stockRef.set(add)
                    .addOnSuccessListener {
                        Log.d(TAG, "Stock created on pharmacies successfully")
                    }.addOnFailureListener {
                        Log.e(TAG, "Error creating stock on pharmacies, e=$it")
                        throw Exception("Error creating stock on pharmacies")
                    }
                transaction.update(stockRef2, "stock", newStock)
            } else {
                Log.e(TAG, "Neither document exists")

                if (amount <= 0) {
                    Log.e(TAG, "Removed stock and entered here")
                } else {
                    val add = hashMapOf(
                        "name" to medicine.name,
                        "description" to medicine.description,
                        "image" to medicine.image,
                        "stock" to amount
                    )
                    stockRef.set(add)
                        .addOnSuccessListener {
                            Log.d(TAG, "Stock created on pharmacies successfully")
                        }.addOnFailureListener {
                            Log.e(TAG, "Error creating stock on pharmacies, e=$it")
                            throw Exception("Error creating stock on pharmacies")
                        }

                    val add2 = hashMapOf(
                        "name" to pharmacy.name,
                        "locationName" to pharmacy.locationName,
                        "longitude" to pharmacy.longitude,
                        "latitude" to pharmacy.latitude,
                        "picture" to pharmacy.picture,
                        "stock" to amount
                    )
                    stockRef2.set(add2)
                        .addOnSuccessListener {
                            Log.d(TAG, "Stock created on medicines successfully")
                        }.addOnFailureListener {
                            Log.e(TAG, "Error creating stock on medicines, e=$it")
                            throw Exception("Error creating stock on medicines")
                        }
                }
            }
        }.addOnSuccessListener {
            Log.d(TAG, "Successfully added stock")
            showToast(R.string.stock_added_successfully)
            finish()
        }.addOnFailureListener {
            showToast(R.string.something_went_wrong)
            Log.e(TAG, "Error adding stock, e=$it")
        }
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}