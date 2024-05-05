package pt.ulisboa.tecnico.cmov.myapplication

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class MedicineInformationPanelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_information_panel)

        val medicine = intent.getParcelableExtra<MedicineMetaData>("medicine")
        if (medicine != null) {
            val medicineName : TextView = findViewById(R.id.medicineName)
            val medicineImage: ImageView = findViewById(R.id.medicineImage)

            medicineName.text = medicine.name
            Glide.with(this@MedicineInformationPanelActivity).load(medicine.image).into(medicineImage)
        }

        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
    }
}