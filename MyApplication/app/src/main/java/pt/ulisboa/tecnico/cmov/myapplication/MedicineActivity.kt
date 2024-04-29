package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MedicineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine)

        createBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        createBottomNavigation()
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