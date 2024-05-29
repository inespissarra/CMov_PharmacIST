package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pt.ulisboa.tecnico.cmov.myapplication.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ConnectivityUtils.initialize(this)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            startActivity(Intent(this, MapsActivity::class.java))
            finish()
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginWithEmail(email, password)
            }
            else {
                showToast(R.string.fill_mandatory_fields)
            }
        }

        binding.signUpText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.loginGuest.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
            finish()
        }

    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast(R.string.login_successful)
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showToast(R.string.authentication_failed)
                }
            }

    }

    private fun showToast(message: Int){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}