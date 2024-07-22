package com.example.cottoninsectcounter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class Upload : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2

    private lateinit var imageView: ImageView
    private lateinit var countTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI elements
        imageView = findViewById(R.id.ImageView)
        countTextView = findViewById(R.id.num)
    }

    fun openCamera(view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                Toast.makeText(this, "No Camera App Available", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun uploadFile(view: View) {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openCamera(findViewById(R.id.open_camera))
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(imageBitmap)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImage: Uri? = data?.data
                    imageView.setImageURI(selectedImage)
                }
            }
        }
    }

    fun resetImage(view: View) {
        imageView.setImageDrawable(null)
        countTextView.text = "0" // Reset the count text view
    }

    fun logout(view: View) {
        clearUserSession()
        val intent = Intent(this, User_Register::class.java)
        startActivity(intent)
        finish()
    }

    private fun clearUserSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun submitImage(view: View) {
        val drawable = imageView.drawable

        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            uploadImageToFirebase(bitmap)
        } else {
            Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebase(bitmap: Bitmap) {
        val farmerName = sharedPreferences.getString("farmerName", "unknown") ?: "unknown"
        val storageReference = storage.reference.child("images/$farmerName/${System.currentTimeMillis()}.jpg")

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        val uploadTask = storageReference.putBytes(data)
        uploadTask.addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                saveImageUrlToDatabase(farmerName, uri.toString()) // Ensure this is called here
                Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageUrlToDatabase(farmerName: String, imageUrl: String) {
        val databaseReference = database.reference.child("farmers").child(farmerName).child("images").push()
        databaseReference.setValue(imageUrl).addOnCompleteListener {
            if (it.isSuccessful) {
                // Image URL saved successfully
            } else {
                // Handle the error
                Toast.makeText(this, "Failed to save image URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun generateAndUpdateRandomCount(view: View) {
        val drawable = imageView.drawable
        if (drawable is BitmapDrawable) {
            val randomNumber = Random.nextInt(4, 48)
            countTextView.text = randomNumber.toString()
        } else {
            Toast.makeText(this, "No image found to generate count", Toast.LENGTH_SHORT).show()
        }
    }

}
