package pt.ulisboa.tecnico.cmov.pharmacist

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BuyMedicineActivity : AppCompatActivity() {

    companion object {
        const val TAG = "BuyMedicineActivity"
    }

    private lateinit var medicine: MedicineMetaData
    private var stock: Int = 0
    private lateinit var pharmacyName: String
    private var db: FirebaseFirestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate initiated")
        setContentView(R.layout.activity_buy_medicine)

        createView()

        Log.d(TAG, "onCreate finished")
    }

    @Suppress("DEPRECATION")
    private fun createView() {
        medicine = intent.getParcelableExtra("medicine")!!
        pharmacyName = intent.getStringExtra("pharmacyName")!!

        // NAME
        val medicineNameTextView: TextView = findViewById(R.id.medicineName)
        if (medicine.name == null) {
            showToast(R.string.something_went_wrong)
            Log.e(TAG, "Error loading medicine's name")
            finish()
            //medicineNameTextView.text = "ErrorName"
        }
        else {
            medicineNameTextView.text = medicine.name
            getStock(medicine.name!!)
        }

        // IMAGE
        val medicineImage: ImageView = findViewById(R.id.medicineImage)
        Glide.with(this@BuyMedicineActivity).load(medicine.image).into(medicineImage)

        // BUY
        val buyButton: Button = findViewById(R.id.buy)
        buyButton.setOnClickListener {
            buyMedicine()
        }
    }

    private fun getStock(medicineName: String) {
        db.collection("pharmacies").document(pharmacyName).
        collection("medicines").
        document(medicineName).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    stock = document.getLong("stock")!!.toInt()
                    val stockAmountTextView: TextView = findViewById(R.id.stockAmount)
                    stockAmountTextView.text = stock.toString()
                }
            }
            .addOnFailureListener {
                showToast(R.string.something_went_wrong)
            }
    }

    private fun buyMedicine() {
        val amountEditText: EditText = findViewById(R.id.amountEdit)
        if (amountEditText.text.toString().takeIf { it.isNotBlank() } != null) {
            val insertedStock: Int = amountEditText.text.toString().toInt()
            getStock(medicine.name!!)
            if (insertedStock > stock) {
                showToast(R.string.not_enough_stock)
            }
            else
                makePurchase(medicine.name!!, insertedStock)
        }
    }

    private fun makePurchase(medicineName: String, amount: Int) {
        Log.d(TAG, "pharmacy name: $pharmacyName\nmedicine name: $medicineName")

        // Delete medicine instead if stock reaches 0
        if (stock - amount == 0) {
            db.collection("pharmacies").document(pharmacyName)
                .collection("medicines")
                .document(medicineName)
                .delete()
                .addOnSuccessListener {
                    db.collection("medicines").document(medicineName)
                        .collection("pharmacies")
                        .document(pharmacyName)
                        .delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "Medicine bought successfully")
                            showToast(R.string.medicine_bought_successfully)
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Error buying medicine")
                            showToast(R.string.error_buying_medicine)
                        }
                }.addOnFailureListener {
                    Log.e(TAG, "Error buying medicine")
                    showToast(R.string.error_buying_medicine)
                }
        } else {
            db.collection("pharmacies").document(pharmacyName)
                .collection("medicines")
                .document(medicineName)
                .update("stock", FieldValue.increment((-amount).toLong()))
                .addOnSuccessListener {
                    db.collection("medicines").document(medicineName)
                        .collection("pharmacies")
                        .document(pharmacyName)
                        .update("stock", FieldValue.increment((-amount).toLong()))
                        .addOnSuccessListener {
                            Log.d(TAG, "Medicine bought successfully")
                            showToast(R.string.medicine_bought_successfully)
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Error buying medicine")
                            showToast(R.string.error_buying_medicine)
                        }
                }.addOnFailureListener {
                    Log.e(TAG, "Error buying medicine")
                    showToast(R.string.error_buying_medicine)
                }
        }
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}