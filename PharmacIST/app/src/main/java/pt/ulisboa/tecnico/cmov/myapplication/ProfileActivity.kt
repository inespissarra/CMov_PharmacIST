package pt.ulisboa.tecnico.cmov.pharmacist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private val user = Firebase.auth.currentUser
    private lateinit var name : String
    private lateinit var email : String
    private lateinit var username: TextView
    private lateinit var logoutButton: Button
    private lateinit var updateUsernameButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    private val TAG = "ProfileActivity"
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        updateUsernameButton = findViewById(R.id.updateUsername)
        updateUsernameButton.setOnClickListener{
            changeUsername()
        }

        username = findViewById(R.id.username)
        username.setOnFocusChangeListener { _, hasFocus ->
            updateUsernameButton.visibility = View.VISIBLE
        }

        logoutButton = findViewById(R.id.logoutButton)
        if (user == null) {
            logoutButton.text = getString(R.string.login_caps)
            username.text = getString(R.string.user)
        }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val favoritePharmaciesRepository = FavoritePharmaciesRepository(this)
            favoritePharmaciesRepository.clearPharmacies()
            startActivity(intent)
            finish()
        }

        if (user != null) {
            email = user.email!!
            val emailView: TextView = findViewById(R.id.userEmail)
            emailView.text = email
            if (user.displayName!=null) {
                name = user.displayName!!
                username.setText(name)
            }
        } else {
            val savedUsername : String? = sharedPreferences.getString("username", "")
            if (!savedUsername.isNullOrEmpty()) {
                username.setText(savedUsername)
            }
        }
    }

    private fun changeUsername() {
        if(user!=null) {
            val inputText = username.text.toString()
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName = inputText
            }.build()
            user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(R.string.username_changed)
                } else {
                    showToast(R.string.something_went_wrong)
                }
            }
        } else {
            val editor = sharedPreferences.edit()
            editor.putString("username", username.text.toString())
            editor.apply()
            showToast(R.string.username_changed)
        }

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        username.clearFocus()
        updateUsernameButton.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        createBottomNavigation()
    }

    private fun createBottomNavigation() {
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavView.selectedItemId = R.id.nav_profile
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_medicine -> {
                    startActivity(Intent(applicationContext, MedicineActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_map -> {
                    //startActivity(Intent(applicationContext, MapsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}