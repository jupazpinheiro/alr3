package com.julia.alr.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.julia.alr.databinding.ActivityPdfAddBinding
import com.julia.alr.models.ModelCategory

class PdfAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private var pdfUri: Uri? = null
    private val TAG = "PDF_ADD_TAG"

    lateinit var wifiManager: WifiManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()
        loadPdfCategories()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Aguarde...")
        progressDialog.setCanceledOnTouchOutside(false)


        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.categoryTv.setOnClickListener {
            categoryPickDialog()
        }
        binding.attachPdfBtn.setOnClickListener {
            //wifiManager.startScan()
            wifiManager= this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (::wifiManager.isInitialized && wifiManager.isWifiEnabled) {
                Toast.makeText(this, "Wifi conectado", Toast.LENGTH_SHORT).show()
                pdfPickIntent()
            }
            else{
                Toast.makeText(this, "Wifi desconectado, uploads apenas em wifi", Toast.LENGTH_SHORT).show()
            }

            //pdfPickIntent()
        }
        binding.submitBtn.setOnClickListener {
            validateData()
        }


    }

    private var title = ""
    private var description = ""
    private var category = ""

    private fun validateData() {
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        category = binding.categoryTv.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Digite o nome do restaurante...", Toast.LENGTH_SHORT).show()
        } else if (description.isEmpty()) {
            Toast.makeText(this, "Digite a descrição do restaurante...", Toast.LENGTH_SHORT).show()
        } else if (category.isEmpty()) {
            Toast.makeText(this, "Escolha o bairro...", Toast.LENGTH_SHORT).show()
        } else if (pdfUri == null) {
            Toast.makeText(this, "Adicione um pdf...", Toast.LENGTH_SHORT).show()
        } else {
            uploadPdfToStorage()
        }


    }

    private fun uploadPdfToStorage() {
        Log.d(TAG, "uploadPdfToStorage: subindo para o banco de dados...")
        progressDialog.setMessage("Subindo PDF...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val filePathName = "Restaurantes/$timestamp"
        val storageReference = FirebaseStorage.getInstance().getReference(filePathName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadPdfToStorage: PDF subindo...")
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedPdfUrl = "${uriTask.result}"

                uploadPdfInfoToDb(uploadedPdfUrl, timestamp)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "uploadPdfToStorage: falha devido ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Falha devido ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun uploadPdfInfoToDb(uploadedPdfUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadPdfToStorage: PDF subindo ao banco de dados...")
        progressDialog.setMessage("Atualizando pfd info")
        val uid = firebaseAuth.uid

        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"
        hashMap["url"] = "$uploadedPdfUrl"
        hashMap["timestamp"] = timestamp
        hashMap["viewsCount"] = 0
        hashMap["downloadCount"] = 0

        val ref = FirebaseDatabase.getInstance().getReference("Restaurantes")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "uploadPdfToDb: atualizado para o bd")
                progressDialog.dismiss()
                Toast.makeText(this, "Subindo...", Toast.LENGTH_SHORT).show()
                pdfUri = null
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "uploadPdfToDb: falha devido ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Falha devido ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun loadPdfCategories() {
        Log.d(TAG, "loadPdfCatgegories: Carregando pdf categorias")
        categoryArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelCategory::class.java)
                    categoryArrayList.add(model!!)
                    Log.d(TAG, "onDataChange: ${model.category}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""

    private fun categoryPickDialog() {
        Log.d(TAG, "categoryPickDialog: Monstrando pdf categoria")
        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices) {
            categoriesArray[i] = categoryArrayList[i].category
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha a categoria")
            .setItems(categoriesArray) { dialog, which ->
                selectedCategoryTitle = categoryArrayList[which].category
                selectedCategoryId = categoryArrayList[which].id
                binding.categoryTv.text = selectedCategoryTitle
                Log.d(TAG, "categoryPickDialog: Categoria selecionada ID: $selectedCategoryId")
                Log.d(TAG, "categoryPickDialog: Categoria selecionada Nome: $selectedCategoryTitle")


            }
            .show()
    }

    private fun pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: começando...")

        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfActivityResultLaucher.launch(intent)
    }

    val pdfActivityResultLaucher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "PDF picked")
                pdfUri = result.data!!.data
            } else {
                Log.d(TAG, "PDF cancelado")
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    )


}