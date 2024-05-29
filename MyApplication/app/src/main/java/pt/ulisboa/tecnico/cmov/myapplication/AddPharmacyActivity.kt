package pt.ulisboa.tecnico.cmov.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage


class AddPharmacyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var pharmacyPhoto : ImageView
    private lateinit var pharmacyPhotoName: TextView
    private lateinit var pharmacyLocation: LatLng
    private lateinit var pharmacyLocationName: TextView
    private lateinit var pharmacyName: EditText
    private var locationSelected: Int = 0

        companion object{
            private const val CAMERA_REQUEST_CODE = 100
            private const val STORAGE_REQUEST_CODE = 101

            private const val TAG = "MAIN_TAG"
        }

    private var imageUri: Uri?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pharmacy)

        pharmacyPhoto = findViewById(R.id.photo)
        pharmacyPhotoName = findViewById<TextView>(R.id.photoField)!!
        pharmacyLocationName = findViewById<TextView>(R.id.addressField)!!
        pharmacyName = findViewById<EditText>(R.id.nameField)!!

        val cameraButton: ImageButton = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener{
            if(checkCameraPermissions()){
                pickImageCamera()
            }
            else{
                requestCameraPermissions()
            }
        }

        val photoField: TextView = findViewById(R.id.photoField)
        photoField.setOnClickListener {
            if(checkCameraPermissions()){
                pickImageCamera()
            }
            else{
                requestCameraPermissions()
            }
        }

        val galleryButton: ImageButton = findViewById(R.id.galleryButton)
        galleryButton.setOnClickListener{
            if(checkStoragePermissions()){
                pickImageGallery()
            }
            else{
                requestStoragePermissions()
            }
        }

        val registerButton: Button = findViewById(R.id.registerBtn)
        registerButton.setOnClickListener{
            registerPharmacy()
        }

        val locationEdit: TextView = findViewById(R.id.addressField)
        locationEdit.setOnClickListener{
            selectLocation()
        }
    }

    private fun selectLocation() {
        val intent = Intent(applicationContext, SelectLocationActivity::class.java)
        if(locationSelected==1){
            intent.putExtra("lat", pharmacyLocation.latitude)
            intent.putExtra("lng", pharmacyLocation.longitude)
        }
        locationActivityResultLauncher.launch(intent)
    }

    @SuppressLint("SetTextI18n")
    private val locationActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ){
            result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data

            val lat = data!!.getDoubleExtra("latitude", Double.MIN_VALUE)
            val lng = data.getDoubleExtra("longitude", Double.MIN_VALUE)
            pharmacyLocation = LatLng(lat, lng)
            val name  = data.getStringExtra("name")
            pharmacyLocationName.setText("$name")
            locationSelected = 1
        }
        else{
            showToast(R.string.canceled)
        }
    }


    private fun registerPharmacy() {
        val name = pharmacyName.text.toString()
        val locationName = pharmacyLocationName.text.toString()
        if(name.takeIf { it.isNotBlank() }!=null
            && locationName.takeIf { it.isNotBlank() }!=null
            && pharmacyPhotoName.text.toString().takeIf { it.isNotBlank() }!=null
        ){
            uploadImage(name, object : UploadCallback {
                override fun onSuccess(downloadUrl: String) {

                    db = Firebase.firestore
                    val pharmacy = hashMapOf(
                        "name" to name,
                        "locationName" to locationName,
                        "latitude" to pharmacyLocation.latitude,
                        "longitude" to pharmacyLocation.longitude,
                        "picture" to downloadUrl
                    )
                    db.collection("pharmacies").document(name)
                        .set(pharmacy)
                        .addOnSuccessListener { showToast(R.string.pharmacy_registered) }
                        .addOnFailureListener { e -> showToast(R.string.error_registering_pharmacy) }
                    finish()
                }
                override fun onFailure(exception: Exception) {
                    showToast(R.string.error_uploading_image)
                }
            })
        }
        else{
            showToast(R.string.fill_mandatory_fields)
        }
    }

    private fun uploadImage(name: String, callback: UploadCallback) {
        storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference
        val imageRef = storageReference.child("pharmacies/$name.jpg");
        val uploadTask = imageRef.putFile(imageUri!!)

        // Listen for success or failure
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Handle successful upload
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                callback.onSuccess(downloadUrl)
            }.addOnFailureListener { exception ->
                // Handle failed download URL retrieval
                callback.onFailure(exception)
            }
        }.addOnFailureListener { exception ->
            // Handle failed upload
            callback.onFailure(exception)
        }
    }

    interface UploadCallback {
        fun onSuccess(downloadUrl: String)
        fun onFailure(exception: Exception)
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
    ){
            result ->
        if(result.resultCode == Activity.RESULT_OK){

            Log.d(TAG, "cameraActivityResult: imageUri: $imageUri")
            pharmacyPhoto.setImageURI(imageUri)

            val name = imageUri?.path?.substringAfterLast('/', "")
            pharmacyPhotoName.setText(" Name: $name")
        }
        else{
            showToast(R.string.canceled)
        }
    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
            result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data

            imageUri = data?.data
            Log.d(TAG, ": imageUri: $imageUri")
            pharmacyPhoto.setImageURI(imageUri)

            val name = imageUri?.path?.substringAfterLast('/', "")
            pharmacyPhotoName.setText(" Name: $name")
        }
        else{
            showToast(R.string.canceled)
        }
    }

    private fun checkCameraPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
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

       when(requestCode){
           CAMERA_REQUEST_CODE ->{
               if(grantResults.isNotEmpty()){
                   val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                   val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                   if(cameraAccepted && storageAccepted){
                       pickImageCamera()
                   }
                   else{
                       showToast(R.string.camera_storage_permissions)
                   }
               }
           }
           STORAGE_REQUEST_CODE ->{
               if(grantResults.isNotEmpty()){

                   if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                       pickImageGallery()
                   }
                   else{
                       showToast(R.string.storage_permissions)
                   }
               }
           }
       }
   }

   private fun showToast(message: Int){
       Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
   }
}