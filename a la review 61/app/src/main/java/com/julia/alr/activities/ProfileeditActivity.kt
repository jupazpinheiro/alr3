package com.julia.alr.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.julia.alr.MyApplication
import com.julia.alr.R
import com.julia.alr.databinding.ActivityProfileeditBinding
import java.lang.Exception

class ProfileeditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileeditBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var imageUri: Uri?= null
    private lateinit var progressDialog: ProgressDialog

    private val CAMERA_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityProfileeditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Aguarde")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth=FirebaseAuth.getInstance()
        loadUserInfo()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.profileIv.setOnClickListener{
            checkPermission(
                Manifest.permission.CAMERA,
                CAMERA_PERMISSION_CODE)
            if (CAMERA_PERMISSION_CODE==100){
            showImageAttachMenu()}

        }
        binding.updateBtn.setOnClickListener{
            validadeData()
        }
    }

    private var name = ""
    private fun validadeData() {
        name=binding.nameEt.text.toString().trim()

        if(name.isEmpty()){
            Toast.makeText(this, "Digite o nome",Toast.LENGTH_SHORT).show()
        }
        else{
            if(imageUri==null){
                updateProfile("")

            }
            else{
                uploadImage()

            }
        }
    }

    private fun uploadImage() {
         progressDialog.setMessage("Carregando imagem")
        progressDialog.show()
        val filePathAndName="ProfileImages/"+firebaseAuth.uid

        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot->
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                    val uploadedImageUrl = "${uriTask.result}"
                updateProfile(uploadedImageUrl)
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Falha no carregamento, devido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Atualizando")
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "$name"
        if (imageUri !=null){
            hashMap["profileImage"] =uploadedImageUrl
        }
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"Sucesso", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Falha na atualização, devido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {
        val ref = FirebaseDatabase.getInstance().getReference( "Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"

                    binding.nameEt.setText(name)

                    try {
                        Glide.with(this@ProfileeditActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)
                    } catch (e: Exception) {

                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
    private fun showImageAttachMenu(){
        val popupMenu = PopupMenu(this, binding.profileIv)
        popupMenu.menu.add(Menu.NONE,0,0,"Camera")
        popupMenu.menu.add(Menu.NONE,1,1,"Galery")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item->
            val id = item.itemId
            if (id==0){
                pickImageCamera()
            }
            else if (id==1){
                pickImageGalery()
            }
            true
        }
    }

    private fun pickImageCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Desscription")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGalery() {
        val intent=Intent(Intent.ACTION_PICK)
        intent.type="image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result->
            if (result.resultCode ==Activity.RESULT_OK){
                val data = result.data
                //imageUri = data!!.data
                binding.profileIv.setImageURI(imageUri)
            }
            else{
                Toast.makeText(this,"Cancelar", Toast.LENGTH_SHORT).show()
            }
        }
    )
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {result->
            if (result.resultCode ==Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data
                binding.profileIv.setImageURI(imageUri)
            }
            else{
                Toast.makeText(this,"Cancelar", Toast.LENGTH_SHORT).show()
            }
        }
    )

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@ProfileeditActivity, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@ProfileeditActivity, arrayOf(permission), requestCode)
            Toast.makeText(this@ProfileeditActivity, "Permissão necessária", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(this@ProfileeditActivity, "Permissão concedida", Toast.LENGTH_SHORT).show()

        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@ProfileeditActivity, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ProfileeditActivity, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
    }


}