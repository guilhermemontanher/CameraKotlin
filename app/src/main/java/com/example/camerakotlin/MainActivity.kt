package com.example.camerakotlin

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.util.Base64
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val CAMERA_REQUEST_CODE = 12345
    private val REQUEST_GALLERY_CAMERA = 54654
    var imageUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        capture.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            capture -> {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ), REQUEST_GALLERY_CAMERA
                        )
                    } else {
                        openCamera()
                    }
                } else {
                    openCamera()
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.example.camerakotlin.android.fileprovider",
                    photoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } else {
                    val resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        grantUriPermission(
                            packageName,
                            photoURI,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        imageUri = image.absolutePath
        return image
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_GALLERY_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {

                    //In case you need to compress and convert to Base64
                    //CompressImage().execute(imageUri) //If you would like to compress the image size before upload it to server, use that class

                    //Add to a ImageView
                    val extras = data?.extras
                    val imageBitmap = extras?.get("data") as Bitmap
                    image.setImageBitmap(imageBitmap)

                    //Get Uri from generated file
                    uriText.text = imageUri
                }
            }
        }
    }

    internal inner class CompressImage : AsyncTask<String, Void, String>() {

        override fun onPreExecute() {
            super.onPreExecute()

        }

        override fun doInBackground(vararg params: String): String {
            val result = getRealPathFromURI(params[0])
            val file = File(result!!)
            val bitmap = decodeImageFile(file)
            val stream = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)


            val image = stream.toByteArray()
            return Base64.encodeToString(image, Base64.DEFAULT).replace("\n".toRegex(), "")
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            try {
                //Here you can upload the compressed image to your server
            } catch (e: JSONException) {
                e.printStackTrace()
            }


        }
    }

    /* To get real path from URI of File */
    private fun getRealPathFromURI(contentURI: String): String? {
        val contentUri = Uri.parse(contentURI)
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        return if (cursor == null) {
            contentUri.path
        } else {
            var res: String? = null
            if (cursor.moveToFirst()) {
                val columnIndex = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                res = cursor.getString(columnIndex)
            }
            cursor.close()
            res
        }
    }

    /* To convert Image to Bitmap */
    fun decodeImageFile(f: File): Bitmap? {
        try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeStream(FileInputStream(f), null, o)
            val size = 150
            var scale = 1
            while (o.outWidth / scale / 2 >= size && o.outHeight / scale / 2 >= size)
                scale *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale

            return BitmapFactory.decodeStream(FileInputStream(f), null, o2)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return null
    }
}

