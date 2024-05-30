package pt.ulisboa.tecnico.cmov.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MedicineInformationPanelActivity : AppCompatActivity() {

    companion object {
        val TAG = "MedicineInformationPanelActivity"
    }

    private lateinit var adapter: ListPharmacyAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var medicineName: TextView
    private lateinit var medicineImage: ImageView
    private lateinit var medicineDescription: TextView
    private lateinit var notificationButton : ImageButton
    private var pharmacyList: ArrayList<Pair<PharmacyMetaData, Pair<Double, Int>>> = ArrayList()
    private var medicineEntry: MedicinePharmacyDBEntryData? = null
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_information_panel)

        recyclerView = findViewById(R.id.pharmacyList)

        val gridLayoutManager = GridLayoutManager(this@MedicineInformationPanelActivity, 1)
        recyclerView.layoutManager = gridLayoutManager

        adapter = ListPharmacyAdapter(this, pharmacyList)
        recyclerView.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        notificationButton = findViewById(R.id.notificationButton)
        notificationButton.setOnClickListener {
            it.isSelected = !it.isSelected
        }

        adapter.onItemClick = {
            val intent = Intent(this, PharmacyInformationPanelActivity::class.java)
            intent.putExtra("pharmacy", it)
            Log.d(MedicineActivity.TAG, "Sending intent with $it to PharmacyInformationPanelActivity")
            startActivity(intent)
        }

        getDataFromIntent()

        setupAdapter()
    }

    private fun getDataFromIntent() {
        medicineEntry = intent.getParcelableExtra<MedicinePharmacyDBEntryData>("medicine")
        userLatitude = intent.getDoubleExtra("userLatitude", 0.0)
        userLongitude = intent.getDoubleExtra("userLongitude", 0.0)

        Log.d(TAG, "Retrieved medicine entry: $medicineEntry")
        Log.d(TAG, "User location: ($userLatitude, $userLongitude)")

        if (medicineEntry != null) {
            medicineName = findViewById(R.id.medicineName)
            medicineImage = findViewById(R.id.medicineImage)
            medicineDescription = findViewById(R.id.medicineDescription)

            if (medicineEntry!!.medicineMetaData?.name != null) {
                medicineName.text = medicineEntry!!.medicineMetaData?.name
            } else {
                medicineName.text = this.getString(R.string.name_not_found)
            }
            Log.d(TAG, "Got medicine name: ${medicineName.text}")
            if (medicineEntry!!.medicineMetaData?.image != null) {
                Log.d(TAG, "Got image: ${medicineEntry!!.medicineMetaData?.image}")
                Glide.with(this@MedicineInformationPanelActivity).load(medicineEntry!!.medicineMetaData?.image).into(medicineImage)
            } else {
                Log.d(TAG, "Got placeholder image")
                Glide.with(this@MedicineInformationPanelActivity).load(R.drawable.placeholder).into(medicineImage)
            }

            if (medicineEntry!!.medicineMetaData?.description != null) {
                medicineDescription.text = medicineEntry!!.medicineMetaData?.description
            } else {
                medicineDescription.text = this.getString(R.string.description_not_found)
            }
        }
    }

    private fun setupAdapter() {
        val pharmacyMap = medicineEntry?.pharmacyMap

        if (pharmacyMap != null) {
            for ((pharmacyName, pharmacyPair) in pharmacyMap) {
                val pharmacyMetaData = pharmacyPair.first
                val stock = pharmacyPair.second
                val distance = pharmacyMetaData.getDistance(userLatitude, userLongitude)
                Log.d(TAG, "Adding ${Pair(pharmacyMetaData, Pair(distance, stock))} to pharmacy list")
                pharmacyList.add(Pair(pharmacyMetaData, Pair(distance, stock)))
            }
            val sortedList = pharmacyList.sortedBy { it.second.first }
            pharmacyList.clear()
            pharmacyList.addAll(sortedList)

            Log.d(TAG, "Final pharmacy list: $pharmacyList")
            adapter.setPharmacyDataList(pharmacyList)
            adapter.notifyDataSetChanged()
            Log.d(TAG, "Finished adapter")
        }
    }
}