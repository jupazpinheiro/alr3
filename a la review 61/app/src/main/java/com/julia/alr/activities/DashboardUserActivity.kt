package com.julia.alr.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.julia.alr.R
import com.julia.alr.databinding.ActivityDashboardUserBinding
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*


class DashboardUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardUserBinding
    private lateinit var firebaseAuth: FirebaseAuth

    val WRITE_REQUEST = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        binding.logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.saveBtn.setOnClickListener {
            checkPermission(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                WRITE_REQUEST)
            if (WRITE_REQUEST==10){
                    val intent =
                        android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    intent.setType("text/plan")
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())+".txt"

                    intent.putExtra(android.content.Intent.EXTRA_TITLE, sdf)

                    startActivityForResult(intent,WRITE_REQUEST)}

        }
        binding.saveBtn2.setOnClickListener {
            checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                WRITE_REQUEST)
            val etxt = this.findViewById<EditText>(R.id.inputEt)
            var value = etxt.text.toString()
            if (WRITE_REQUEST==10){
                writeToFile(value)}
        }
        binding.saveBtn3.setOnClickListener {
            val etxt = this.findViewById<EditText>(R.id.inputEt)
            val masterK = MasterKey.Builder(this, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val sdf = "Encrypto"+SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())+".txt"
            val writer = File(this.filesDir, sdf)
            var encripto = EncryptedFile.Builder(this, writer, masterK, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
            var outEncript = encripto.openFileOutput()
            outEncript.write(etxt.toString().toByteArray())
            outEncript.close()

        }
    }

    private fun writeToFile(str:String) {
        try{
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())+".txt"
            val writer = FileWriter(File(this.filesDir, sdf))
            writer.write("")
            writer.write(str)
            writer.close()
        }
        catch(e:Exception){
            print(e.message)

        }
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null){
            //startActivity(Intent(this, MainActivity::class.java))
            //finish()
            binding.subTitleTv.text = "Não logado"

            binding.profileBtn.visibility=View.GONE
            binding.logoutBtn.visibility=View.GONE
        }
        else{
            val email = firebaseUser.email
            binding.subTitleTv.text = email

            binding.profileBtn.visibility=View.GONE
            binding.logoutBtn.visibility=View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i ("DR4", "onActivityResult resultCode"+resultCode)
        if (requestCode == WRITE_REQUEST && resultCode == RESULT_OK){
            val txtText = this.findViewById<EditText>(R.id.inputEt)
            val fos = getContentResolver().openOutputStream(data?.getData()!!)
            Log.i("DR4", "onActivityResult fos ="+ fos.toString())
            fos?.close()
            txtText.setText(null)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@DashboardUserActivity, "Permitido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DashboardUserActivity, "Negada", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@DashboardUserActivity, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(this@DashboardUserActivity, arrayOf(permission), requestCode)
        } else {
            Toast.makeText(this@DashboardUserActivity, "Já foi permitido", Toast.LENGTH_SHORT).show()
        }
    }
}