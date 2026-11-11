package com.gpsmapcamera.app

import android.content.ContentUris
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF101820.toInt()) }
        root.addView(TextView(this).apply { text = "  GPS PHOTO GALLERY"; textSize = 18f; setTextColor(-1); setPadding(12,18,12,18) })
        val grid = GridView(this).apply { numColumns = 2; verticalSpacing = 8; horizontalSpacing = 8; setPadding(8,8,8,8) }
        root.addView(grid, LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
        val photos = loadPhotos()
        if (photos.isEmpty()) { root.addView(TextView(this).apply { text = "No GPS Camera photos yet"; setTextColor(-1); gravity = 17; setPadding(12,30,12,30) }) }
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = photos.size
            override fun getItem(position: Int) = photos[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
                val image = (convertView as? ImageView) ?: ImageView(this@GalleryActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                image.layoutParams = AbsListView.LayoutParams(-1, 230)
                image.setImageURI(photos[position])
                image.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(photos[position],"image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                return image
            }
        }
    }
    private fun loadPhotos(): List<Uri> {
        val list = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = if (android.os.Build.VERSION.SDK_INT >= 29) "${MediaStore.Images.Media.RELATIVE_PATH}=?" else "${MediaStore.Images.Media.DATA} LIKE ?"
        val arg = if (android.os.Build.VERSION.SDK_INT >= 29) "Pictures/GPSMapCamera/" else "%/GPSMapCamera/%"
        contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,projection,selection,arrayOf(arg),"${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext()) list.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,c.getLong(idCol)))
        }
        return list
    }
}
