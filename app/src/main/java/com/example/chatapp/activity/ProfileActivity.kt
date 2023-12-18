package com.example.chatapp.activity



import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.util.UUID

class ProfileActivity : AppCompatActivity() {


    private lateinit var firebaseUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    private var filePath: Uri? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.uid)

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val etUserName: EditText = findViewById(R.id.etUserName)
                val userImage: ImageView = findViewById(R.id.userImage)
                val user = snapshot.getValue(User::class.java)
                etUserName.setText(user!!.userName)

                if (user.profileImage == "") {
                    userImage.setImageResource(R.drawable.profile_image)
                } else {
                    Glide.with(this@ProfileActivity).load(user.profileImage).into(userImage)
                }
            }
        })

        val userImage: ImageView = findViewById(R.id.userImage)
        val imgBack: ImageView = findViewById(R.id.imgBack)
        imgBack.setOnClickListener {
            finish()
        }

        userImage.setOnClickListener {
            chooseImage()
        }

        val btnSave: Button = findViewById(R.id.btnSave)
        val progressBar:ProgressBar=findViewById(R.id.progressBar)
        btnSave.setOnClickListener {
            uploadImage()
            progressBar.visibility = View.VISIBLE
        }

        val btnLogout: Button = findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener {
            // Log out the user and redirect to LoginActivity
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
            finish()
        }


        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Handle the result here
                val data: Intent? = result.data
                if (data != null && data.data != null) {
                    filePath = data.data
                    try {
                        val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                        val userImage: ImageView = findViewById(R.id.userImage)
                        userImage.setImageBitmap(bitmap)
                        val btnSave: Button = findViewById(R.id.btnSave)
                        btnSave.visibility = View.VISIBLE
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image"))
    }


    private fun uploadImage() {
        val btnSave: Button = findViewById(R.id.btnSave)
        val etUserName:EditText = findViewById(R.id.etUserName)
        if (filePath != null) {
            val progressBar:ProgressBar=findViewById(R.id.progressBar)
            var ref: StorageReference = storageRef.child("image/" + UUID.randomUUID().toString())
            ref.putFile(filePath!!)
                .addOnSuccessListener {

                    val hashMap:HashMap<String,String> = HashMap()
                    hashMap.put("userName",etUserName.text.toString())
                    hashMap.put("profileImage",filePath.toString())
                    databaseReference.updateChildren(hashMap as Map<String, Any>)
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
                    btnSave.visibility = View.GONE
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT).show()

                }

        }
    }


}