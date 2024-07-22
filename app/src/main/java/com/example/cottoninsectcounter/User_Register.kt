package com.example.cottoninsectcounter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase

class User_Register : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        // Check if user details are already saved
        checkUserSession()
    }

    private fun checkUserSession() {
        val farmerName = sharedPreferences.getString("farmerName", null)
        val mobileNumber = sharedPreferences.getString("mobileNumber", null)
        if (farmerName != null && mobileNumber != null) {
            // com.example.cottoninsectcounter.User is already logged in, redirect to the next activity
            val intent = Intent(this, Upload::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun Next(view: View) {
        val name = findViewById<EditText>(R.id.name).text.toString()
        val age = findViewById<EditText>(R.id.Age).text.toString()
        val mobile = findViewById<EditText>(R.id.login_mobile).text.toString()
        val place = findViewById<EditText>(R.id.place).text.toString()

        // Save user session
        saveUserSession(name, mobile)

        // Save to Firebase
        saveToFirebase(name, age, mobile, place)

        val intent = Intent(this, Upload::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveUserSession(farmerName: String, mobileNumber: String) {
        val editor = sharedPreferences.edit()
        editor.putString("farmerName", farmerName)
        editor.putString("mobileNumber", mobileNumber)
        editor.apply()
    }

    private fun saveToFirebase(name: String, age: String, mobile: String, place: String) {
        val user = User(name, age, mobile, place)
        val databaseReference = database.reference.child("farmers").child(name)
        databaseReference.setValue(user).addOnCompleteListener {
            if (it.isSuccessful) {
                // Data saved successfully
            } else {
                // Handle the error
            }
        }
    }
}
