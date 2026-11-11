package com.gpsmapcamera.app

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var locationText: TextView
    private lateinit var dateText: TextView
    private lateinit var statusText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var lat: Double? = null
    private var lon: Double? = null
    private var addressText = "Getting location…"
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.CAMERA] == true) startCamera()
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) updateLocation()
        if (grants[Manifest.permission.CAMERA] != true) statusText.text = "Camera permission is required to take photos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        buildScreen()
        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (needed.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) permissionLauncher.launch(needed)
        else { startCamera(); updateLocation() }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(16,24,32)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(18,14,18,14) }
        top.addView(TextView(this).apply { text = "GPS MAP CAMERA"; setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply { text = "● LIVE GPS"; setTextColor(Color.rgb(87,220,164)); textSize = 12f })
        root.addView(top)
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        previewView = PreviewView(this).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        frame.addView(previewView, FrameLayout.LayoutParams(-1,-1))
        val stamp = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(16,12,16,12); setBackgroundColor(0xB8000000.toInt())
        }
        locationText = TextView(this).apply { text = "Waiting for GPS location…"; setTextColor(Color.WHITE); textSize = 13f; maxLines = 3 }
        dateText = TextView(this).apply { text = nowString(); setTextColor(Color.WHITE); textSize = 12f }
        stamp.addView(locationText); stamp.addView(dateText)
        val stampParams = FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply { setMargins(14,14,14,14) }
        frame.addView(stamp,stampParams)
        root.addView(frame, LinearLayout.LayoutParams(-1,0,1f))
        statusText = TextView(this).apply { text = "GPS ready when location is available"; setTextColor(0xFFB8C7D1.toInt()); textSize = 12f; gravity = Gravity.CENTER; setPadding(8,8,8,8) }
        root.addView(statusText)
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL; setPadding(12,12,12,18) }
        val gallery = Button(this).apply { text = "Gallery"; setOnClickListener { startActivity(Intent(this@MainActivity, GalleryActivity::class.java)) } }
        val capture = Button(this).apply { text = "  ●  CAPTURE  "; textSize = 16f; setOnClickListener { takePhoto() } }
        val map = Button(this).apply { text = "Map"; setOnClickListener { openMap() } }
        controls.addView(gallery); controls.addView(capture); controls.addView(map)
        root.addView(controls)
        setContentView(root)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val captureUseCase = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                imageCapture = captureUseCase
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, captureUseCase)
            } catch (e: Exception) { statusText.text = "Unable to start camera: ${e.localizedMessage}" }
        }, ContextCompat.getMainExecutor(this))
    }

    @Suppress("MissingPermission")
    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        statusText.text = "Finding GPS location…"
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
            if (loc == null) { statusText.text = "Location unavailable. Turn on device location and retry."; return@addOnSuccessListener }
            lat = loc.latitude; lon = loc.longitude
            addressText = "${loc.latitude}, ${loc.longitude}"
            locationText.text = "${loc.latitude.format(6)}, ${loc.longitude.format(6)}"
            statusText.text = "GPS accuracy: ${loc.accuracy.toInt()} m"
            if (Build.VERSION.SDK_INT >= 33) {
                Geocoder(this, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1) { results ->
                    if (results.isNotEmpty()) runOnUiThread {
                        addressText = formatAddress(results[0])
                        locationText.text = "$addressText\n${loc.latitude.format(6)}, ${loc.longitude.format(6)}"
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                cameraExecutor.execute {
                    try {
                        @Suppress("DEPRECATION")
                        val results = Geocoder(this, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!results.isNullOrEmpty()) { addressText = formatAddress(results[0]); runOnUiThread { locationText.text = "$addressText\n${loc.latitude.format(6)}, ${loc.longitude.format(6)}" } }
                    } catch (_: Exception) { }
                }
            }
        }.addOnFailureListener { statusText.text = "GPS error: ${it.localizedMessage}" }
    }

    private fun formatAddress(a: Address): String = listOfNotNull(a.subLocality, a.locality, a.subAdminArea, a.adminArea, a.postalCode, a.countryName).distinct().joinToString(", ").ifBlank { "Address unavailable" }

    private fun takePhoto() {
        val capture = imageCapture ?: run { statusText.text = "Camera is not ready"; return }
        val temp = File(cacheDir, "gps_${System.currentTimeMillis()}.jpg")
        statusText.text = "Capturing photo…"
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(temp).build(), cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(temp.absolutePath) ?: throw IllegalStateException("Could not decode photo")
                    val oriented = applyExifRotation(temp, bitmap)
                    val stamped = drawStamp(oriented)
                    val saved = saveToGallery(stamped)
                    temp.delete()
                    runOnUiThread { statusText.text = if (saved != null) "Photo saved to Pictures/GPSMapCamera" else "Could not save photo"; Toast.makeText(this@MainActivity, if (saved != null) "Photo saved" else "Save failed", Toast.LENGTH_SHORT).show() }
                    updateLocation()
                } catch (e: Exception) { runOnUiThread { statusText.text = "Photo error: ${e.localizedMessage}" } }
            }
            override fun onError(exception: ImageCaptureException) { runOnUiThread { statusText.text = "Capture failed: ${exception.localizedMessage}" } }
        })
    }

    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
        val orientation = ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) { ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    }

    private fun drawStamp(source: Bitmap): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val scale = result.width / 1080f
        val pad = 26f * scale
        val titleSize = 30f * scale
        val detailSize = 23f * scale
        val lines = mutableListOf("GPS MAP CAMERA", addressText, if (lat != null && lon != null) "Lat ${lat!!.format(6)}   Long ${lon!!.format(6)}" else "GPS location unavailable", nowString())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = detailSize; typeface = Typeface.create("sans-serif",Typeface.NORMAL) }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = titleSize; typeface = Typeface.create("sans-serif-medium",Typeface.BOLD) }
        val maxWidth = result.width - 2*pad
        val wrapped = mutableListOf<String>()
        for ((idx,line) in lines.withIndex()) {
            val p = if (idx == 0) titlePaint else paint
            var current = ""
            for (word in line.split(" ")) {
                val test = if (current.isEmpty()) word else "$current $word"
                if (p.measureText(test) > maxWidth && current.isNotEmpty()) { wrapped.add(current); current = word } else current = test
            }
            if (current.isNotEmpty()) wrapped.add(current)
        }
        val lineHeight = 1.45f * detailSize
        val height = pad*2 + titleSize*1.5f + (wrapped.size-1)*lineHeight
        val top = result.height-height
        val bg = Paint().apply { color = 0xCC101820.toInt() }
        canvas.drawRoundRect(pad/2,top,result.width-pad/2,result.height-pad/2,18f*scale,18f*scale,bg)
        var y = top+pad+titleSize
        for ((i,line) in wrapped.withIndex()) {
            val p = if (i==0) titlePaint else paint
            canvas.drawText(line,pad,y,p)
            y += if (i==0) titleSize*1.35f else lineHeight
        }
        return result
    }

    private fun saveToGallery(bitmap: Bitmap): Uri? {
        val name = "GPS_${SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,name); put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/GPSMapCamera")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: return null
        contentResolver.openOutputStream(uri)?.use { if (!bitmap.compress(Bitmap.CompressFormat.JPEG,94,it)) throw IllegalStateException("JPEG encoding failed") }
        return uri
    }

    private fun openMap() {
        val latitude = lat; val longitude = lon
        if (latitude == null || longitude == null) { updateLocation(); Toast.makeText(this,"Waiting for GPS location",Toast.LENGTH_SHORT).show(); return }
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        startActivity(Intent(Intent.ACTION_VIEW,uri))
    }

    private fun nowString() = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a",Locale.getDefault()).format(Date())
    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
    private fun Double.format(digits: Int) = String.format(Locale.US,"%.${digits}f",this)
}
