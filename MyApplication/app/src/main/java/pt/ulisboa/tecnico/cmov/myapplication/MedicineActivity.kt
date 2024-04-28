package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MedicineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine)

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