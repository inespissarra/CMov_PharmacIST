package pt.ulisboa.tecnico.cmov.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pt.ulisboa.tecnico.cmov.myapplication.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_sign_up)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        binding.signUpButton.setOnClickListener {
            val email = binding.signUpEmail.text.toString()
            val username = binding.signUpUsername.text.toString()
            val password = binding.signUpPassword.text.toString()

            if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                signUpWithEmail(email, username, password)
            }
            else {
                Toast.makeText(this, "Please enter email, username and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }

    private fun signUpWithEmail(email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    /*Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)*/
                    val profileUpdates = UserProfileChangeRequest.Builder().apply {
                        displayName = username
                    }.build()
                    auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "SignUp Successful", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "SignUp Successful.\nSomething went wrong creating the username. You can change it later", Toast.LENGTH_SHORT).show()
                        }
                    }
                    //val intent = Intent(this, LoginActivity::class.java)
                    //startActivity(intent)
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    //Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "SignUp failed", Toast.LENGTH_SHORT).show()
                    //updateUI(null)
                }
            }
    }
}