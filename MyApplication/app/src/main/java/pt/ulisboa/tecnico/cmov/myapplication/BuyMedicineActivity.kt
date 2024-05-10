package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class BuyMedicineActivity : AppCompatActivity() {

    companion object {
        val TAG = "BuyMedicineActivity"
    }

    private var medicineName: String? = null
    private var availableStock: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate initiated")
        setContentView(R.layout.activity_buy_medicine)

        createMedicineName()
        createMedicineImage()
        val intentStock = intent.getStringExtra("stock")
        availableStock = intentStock?.toInt() ?: 0
        createStockAmount(stock = intentStock!!)

        createBuyButton()

        Log.d(TAG, "onCreate finished")
    }

    private fun createMedicineName() {
        val medicineNameTextView: TextView = findViewById(R.id.medicineName)
        val intentMedicineName = intent.getCharSequenceExtra("medicineName")
        if (intentMedicineName == null) medicineNameTextView.text = "ErrorName"
        else {
            medicineNameTextView.text = intentMedicineName
            medicineName = intentMedicineName.toString()
        }
        if (intentMedicineName == "ErrorName") Log.e(PharmacyInformationPanelActivity.TAG, "Error loading medicine's name")
    }

    private fun createMedicineImage() {
        val medicineImage: ImageView = findViewById(R.id.pharmacyImage)
        medicineImage.setImageDrawable(
            // TODO: Retrieve medicine's image from database (API)
            ContextCompat.getDrawable(this, R.drawable.placeholder)
        )
    }

    private fun createStockAmount(stock: String) {
        val stockAmountTextView: TextView = findViewById(R.id.stockAmount)
        stockAmountTextView.text = stock
    }

    private fun createBuyButton() {
        val buyButton: Button = findViewById(R.id.buy)
        buyButton.setOnClickListener {
            buyMedicine()
        }
    }

    private fun buyMedicine() {
        val amountEditText: EditText = findViewById(R.id.amountEdit)
        if (amountEditText.text.toString().takeIf { it.isNotBlank() } != null) {
            val insertedStock: Int = amountEditText.text.toString().toInt()
            // TODO: Poll database for amount
            if (insertedStock < availableStock) {
                showToast("There is not enough stock")
            }
            // TODO: Send purchase to database

            // TODO: Validate purchase
            if (true) {
                Log.d(TAG, "Medicine bought successfully")
                showToast("Medicine bought successfully")
                finish()
            } else {
                Log.e(TAG, "Error buying medicine")
                showToast("Error buying medicine")
            }
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}