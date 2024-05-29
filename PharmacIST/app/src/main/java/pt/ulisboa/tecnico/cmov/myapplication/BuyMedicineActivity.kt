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
        val TAG = "BuyMedicineActivity"
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

    private fun createView() {
        medicine = intent.getParcelableExtra<MedicineMetaData>("medicine")!!
        stock = intent.getIntExtra("stock", 0)
        pharmacyName = intent.getStringExtra("pharmacyName")!!

        // NAME
        val medicineNameTextView: TextView = findViewById(R.id.medicineName)
        if (medicine.name == null) medicineNameTextView.text = "ErrorName"
        else {
            medicineNameTextView.text = medicine.name
        }
        if (medicine.name == "ErrorName") Log.e(PharmacyInformationPanelActivity.TAG, "Error loading medicine's name")

        // IMAGE
        val medicineImage: ImageView = findViewById(R.id.medicineImage)
        Glide.with(this@BuyMedicineActivity).load(medicine.image).into(medicineImage)

        // STOCK
        val stockAmountTextView: TextView = findViewById(R.id.stockAmount)
        stockAmountTextView.text = stock.toString()

        // BUY
        val buyButton: Button = findViewById(R.id.buy)
        buyButton.setOnClickListener {
            buyMedicine()
        }
    }

    private fun buyMedicine() {
        val amountEditText: EditText = findViewById(R.id.amountEdit)
        if (amountEditText.text.toString().takeIf { it.isNotBlank() } != null) {
            val insertedStock: Int = amountEditText.text.toString().toInt()
            if (insertedStock > stock) {
                showToast(R.string.not_enough_stock)
            }
            makePurchase(medicine.name!!, insertedStock)
        }
    }

    private fun makePurchase(medicineName: String, amount: Int){
        Log.d(TAG, "pharmacy name: $pharmacyName\nmedicine name: $medicineName")
        db.collection("stock").document(pharmacyName + "_" + medicineName)
            .update("amount", FieldValue.increment((-amount).toLong()))
            .addOnSuccessListener {
                Log.d(TAG, "Medicine bought successfully")
                showToast(R.string.medicine_bought_successfully)
                finish()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error buying medicine")
                showToast(R.string.error_buying_medicine)

            }
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}