package pt.ulisboa.tecnico.cmov.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class AddPharmacyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

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

        createBottomNavigation()

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
                requestCameraPermission()
            }
        }

        val photoField: TextView = findViewById(R.id.photoField)
        photoField.setOnClickListener {
            if(checkCameraPermissions()){
                pickImageCamera()
            }
            else{
                requestCameraPermission()
            }
        }

        val galleryButton: ImageButton = findViewById(R.id.galleryButton)
        galleryButton.setOnClickListener{
            if(checkStoragePermissions()){
                pickImageGallery()
            }
            else{
                requestStoragePermission()
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
            pharmacyLocationName.setText("Location: $name")
            locationSelected = 1
        }
        else{
            Toast.makeText(this, "Canceled...!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun registerPharmacy() {
        var name = pharmacyName.text.toString()
        var locationName = pharmacyLocationName.text.toString()
        if(name.takeIf { it.isNotBlank() }!=null
            && locationName.takeIf { it.isNotBlank() }!=null
            && pharmacyPhotoName.text.toString().takeIf { it.isNotBlank() }!=null
        ){
            //uploadImage(name)

            db = Firebase.firestore
            val pharmacy = hashMapOf(
                "name" to name,
                "locationName" to locationName,
                "latitude" to pharmacyLocation.latitude,
                "longitude" to pharmacyLocation.longitude
            )
            db.collection("pharmacies").document(name)
                .set(pharmacy)
                .addOnSuccessListener { showToast("Successfully registered pharmacy")}
                .addOnFailureListener { e -> showToast("Something went wrong :( " + e)}
            finish()
        }
        else{
            showToast("Please fill in all fields with (*)")
        }
    }

//    private fun uploadImage(name: String) {
//        var storage = Firebase.storage
//        val storageReference = storage.reference
//        storageReference.putFile(imageUri!!)
//    }

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
            val data = result.data

            Log.d(TAG, "cameraActivityResult: imageUri: $imageUri")
            pharmacyPhoto.setImageURI(imageUri)
            pharmacyPhotoName.setText("$imageUri")

            val name = imageUri?.path?.substringAfterLast('/', "")
            pharmacyPhotoName.setText(" Name: " + name)
        }
        else{
            Toast.makeText(this, "Canceled...!", Toast.LENGTH_SHORT).show()
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
            pharmacyPhotoName.setText(" Name: " + name)
        }
        else{
            Toast.makeText(this, "Canceled...!", Toast.LENGTH_SHORT).show()
        }
    }

   private fun checkCameraPermissions(): Boolean{
       val resultCamera = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
       val resultStorage = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
       return resultCamera && resultStorage
   }

   private fun requestCameraPermission(){
       ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_REQUEST_CODE)
       return
   }

    private fun checkStoragePermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_REQUEST_CODE)
        return
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
                       showToast("Camera and Storage permissions are required")
                   }
               }
           }
           STORAGE_REQUEST_CODE ->{
               if(grantResults.isNotEmpty()){

                   if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                       pickImageGallery()
                   }
                   else{
                       showToast("Storage permission are required")
                   }
               }
           }
       }
   }

   private fun showToast(message: String){
       Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
   }
}