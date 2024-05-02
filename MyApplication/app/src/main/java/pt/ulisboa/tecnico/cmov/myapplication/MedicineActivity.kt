package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.ulisboa.tecnico.cmov.myapplication.databinding.ActivityMedicineBinding
import java.util.Locale

class MedicineActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var binding: ActivityMedicineBinding
    private lateinit var medicineList: ArrayList<MedicineMetaData>
    private lateinit var adapter: MedicineAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = ActivityMedicineBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_medicine)
        //setContentView(binding.root)

        createBottomNavigation()

        recyclerView = findViewById(R.id.medicineRecyclerView)
        searchView = findViewById(R.id.search_medicine)

        val gridLayoutManager = GridLayoutManager(this@MedicineActivity, 1)
        //binding.medicineRecyclerView.layoutManager = gridLayoutManager
        recyclerView.layoutManager = gridLayoutManager

        // loading pop-up
        //displayProgressPopUp()

        medicineList = ArrayList()
        adapter = MedicineAdapter(this@MedicineActivity, medicineList)
        //binding.medicineRecyclerView.adapter = adapter
        recyclerView.adapter = adapter

        eventChangeListener()

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchMedicine(newText)
                return true
            }

        })
    }

    private fun searchMedicine(query: String?) {
        if (query != null) {
            val searchList = ArrayList<MedicineMetaData>()
            for (medicine in medicineList) {
                if (medicine.name?.lowercase()?.contains(query.lowercase(Locale.getDefault())) == true) {
                    searchList.add(medicine)
                }
            }

            if (searchList.isEmpty()) {
                Toast.makeText(this, "No medicine found", Toast.LENGTH_SHORT).show()
            }
            else {
                adapter.searchMedicineList(searchList)
                recyclerView.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        createBottomNavigation()
    }

    private fun eventChangeListener() {

        db = FirebaseFirestore.getInstance()
        db.collection("medicines").get().
                addOnSuccessListener {
                    if (!it.isEmpty) {
                        for (data in it.documents) {
                            val medicine: MedicineMetaData? = data.toObject(MedicineMetaData::class.java)
                            if (medicine != null) {
                                medicineList.add(medicine)
                            }
                        }
                        recyclerView.adapter = MedicineAdapter(this@MedicineActivity, medicineList)
                    }
                }.
                addOnFailureListener {
                    Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                }

    }

    private fun displayProgressPopUp() {
        val builder = AlertDialog.Builder(this@MedicineActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val dialog = builder.create()
        dialog.show()
    }

    private fun createBottomNavigation() {
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavView.selectedItemId = R.id.nav_medicine
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicine -> true
                R.id.nav_map -> {
                    //startActivity(Intent(applicationContext, MapsActivity::class.java))
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
}