package com.oxxy.takenoteswithimages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.oxxy.takenoteswithimages.databinding.ActivityMainBinding
import com.oxxy.takenoteswithimages.databinding.ActivityNoteBinding
import java.io.ByteArrayOutputStream

class NoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap: Bitmap? = null
    private lateinit var database: SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Notes", MODE_PRIVATE, null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")){
            binding.titleText.setText("")
            binding.noteText.setText("")
            binding.saveButton.visibility= View.VISIBLE
        }else{
            binding.saveButton.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val cursor = database.rawQuery("SELECT * FROM notes WHERE id = ?", arrayOf(selectedId.toString()))

            val noteTitleIndex = cursor.getColumnIndex("notetitle")
            val noteTextIndex = cursor.getColumnIndex("notetext")
            val imageIndex = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.titleText.setText(cursor.getString(noteTitleIndex))
                binding.noteText.setText(cursor.getString(noteTextIndex))

                val byteArray = cursor.getBlob(imageIndex)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0 , byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }

            cursor.close()

        }

    }

    fun saveButtonHandler(view: View) {
        val noteTitle = binding.titleText.text.toString()
        val noteText = binding.noteText.text.toString()
        if(selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, maximumSize = 300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, notetitle VARCHAR, notetext VARCHAR, image BLOB)")
                val sqlString = "INSERT INTO notes (notetitle, notetext, image) VALUES (?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,noteTitle)
                statement.bindString(2,noteText)
                statement.bindBlob(3, byteArray)
                statement.execute()
            }catch (e: Exception){
                e.printStackTrace()
            }

            val intent = Intent(this@NoteActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }
    }

    private fun makeSmallerBitmap(image: Bitmap,maximumSize: Int):Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio:Double = width.toDouble()/height.toDouble()

        if(bitmapRatio>1){
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        }else{
            height = maximumSize
            val scaledWidth = height* bitmapRatio
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View) {

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(
                        view,
                        "Permission needed for using gallery",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give Permission", View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }
        }else{
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(
                        view,
                        "Permission needed for using gallery",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give Permission", View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }
        }

    }

    private fun registerLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        val imageData = intentFromResult.data
                        //binding.imageView.setImageURI(imageData)
                        if (imageData != null) {
                            if (Build.VERSION.SDK_INT >= 28) {
                                try {
                                    if (Build.VERSION.SDK_INT >= 28) {

                                        val source = ImageDecoder.createSource(
                                            this@NoteActivity.contentResolver,
                                            imageData
                                        )
                                        selectedBitmap = ImageDecoder.decodeBitmap(source)
                                        binding.imageView.setImageBitmap(selectedBitmap)
                                    } else {
                                        selectedBitmap = MediaStore.Images.Media.getBitmap(
                                            contentResolver,
                                            imageData
                                        )
                                        binding.imageView.setImageBitmap(selectedBitmap)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

            }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result->
            if(result){
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(this@NoteActivity, "Permission needed.", Toast.LENGTH_LONG).show()
            }
        }
    }
}