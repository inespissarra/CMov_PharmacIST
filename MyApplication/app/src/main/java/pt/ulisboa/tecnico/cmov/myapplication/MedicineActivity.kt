package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.ulisboa.tecnico.cmov.myapplication.databinding.ActivityMedicineBinding
import java.util.Locale

class MedicineActivity : AppCompatActivity() {

    companion object {
        val TAG = "MedicineActivity"
    }

    private var db: FirebaseFirestore = Firebase.firestore

    private lateinit var binding: ActivityMedicineBinding
    private lateinit var adapter: MedicineAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private var medicineList: ArrayList<MedicineMetaData> = ArrayList()
    private var filteredList: ArrayList<MedicineMetaData> = ArrayList()
    private var lastFetch: DocumentSnapshot? = null
    private var isLoading: Boolean = false
    private var hasMoreData: Boolean = true
    private var inSearch: Boolean = false
    private var prevQuerySize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

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

        adapter = MedicineAdapter(this@MedicineActivity, filteredList)
        //binding.medicineRecyclerView.adapter = adapter
        recyclerView.adapter = adapter

        adapter.onItemClick = {
            val intent = Intent(this, MedicineInformationPanelActivity::class.java)
            intent.putExtra("medicine", it)
            startActivity(intent)
        }

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.length > prevQuerySize) {
                        // User added text
                        searchMedicine(newText, true)
                    } else if (it.length < prevQuerySize) {
                        // User deleted text
                        // This assumes the user typed the query one by one and thus
                        // substrings were already queried. If the user wrote it
                        // in one go, e.g. by pasting text, this will miss some results
                        searchMedicine(newText, false)
                    }
                    prevQuerySize = it.length
                }
                return true
            }

        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading && !inSearch && hasMoreData) {
                    loadData()
                }
            }
        })

        loadData()

        Log.d(TAG, "onCreate finished")
    }

    private fun loadData() {
        Log.d(TAG, "Loading data from DB")
        isLoading = true
        var query: Query

        query = db.collection("medicines").limit(10).orderBy("name")

        lastFetch?.let {
            query = query.startAfter(it)
        }

        query.get().addOnSuccessListener { documents ->
            val initialSize = filteredList.size
            for (document in documents) {
                val item = document.toObject(MedicineMetaData::class.java)
                if (!medicineList.contains(item)) {
                    medicineList.add(item)
                }
                if (!filteredList.contains(item)) {
                    filteredList.add(item)
                }
            }
            if (!documents.isEmpty) {
                lastFetch = documents.documents[documents.size() - 1]
            }
            if (initialSize == filteredList.size) {
                hasMoreData = false
            } else {
                Log.d(TAG, "Notifying change from $initialSize to ${filteredList.size}")
                adapter.notifyItemRangeChanged(initialSize, filteredList.size)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error loading data from DB", Toast.LENGTH_SHORT).show()
        }

        isLoading = false
        Log.d(TAG, "Finished loading data from DB, medicineList=$medicineList, filteredList=$filteredList")
    }

    private fun searchMedicine(query: String?, new: Boolean) {
        filteredList.clear()
        if (query != null) {
            inSearch = true

            medicineList.filterTo(filteredList) {
                it.name?.lowercase()?.contains(query.lowercase(Locale.getDefault())) == true
            }

            if (query.length >= 2 && new && hasMoreData) {
                db.collection("medicines")
                    .orderBy("name")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .get()
                    .addOnSuccessListener { documents ->
                            for (document in documents) {
                                val medicine = document.toObject(MedicineMetaData::class.java)
                                if (!filteredList.contains(medicine)) {
                                    filteredList.add(medicine)
                                    if (!medicineList.contains(medicine)) {
                                        medicineList.add(medicine)
                                    }
                                }
                            }
                        }.
                        addOnFailureListener {
                            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                        }
            }
            if (filteredList.isEmpty() && new) {
                Toast.makeText(this, "No medicine found", Toast.LENGTH_SHORT).show()
            }
        } else {
            inSearch = false
            filteredList.addAll(medicineList)
        }
        adapter.notifyDataSetChanged()
    }

    private fun eventChangeListener() {

        db = Firebase.firestore
        db.collection("medicines").get().
                addOnSuccessListener {
                    if (!it.isEmpty) {
                        for (data in it.documents) {
                            val medicine: MedicineMetaData? = data.toObject(MedicineMetaData::class.java)
                            if (medicine != null) {
                                medicineList.add(medicine)
                            }
                        }
                        adapter.setMedicineList( medicineList)
                        recyclerView.adapter = adapter
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
                    startActivity(Intent(applicationContext, MapsActivity::class.java))
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