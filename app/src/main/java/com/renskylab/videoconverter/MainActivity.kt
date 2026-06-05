package com.renskylab.videoconverter

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
    }

    fun exportVideoToGallery(videoFile: File) {
        val resolver = contentResolver
        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val videoDetails = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "RenConverter_${videoFile.name}")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RenVideoConverter")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val videoContentUri = resolver.insert(videoCollection, videoDetails)
        videoContentUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(videoFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    videoDetails.clear()
                    videoDetails.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, videoDetails, null, null)
                }
                com.google.android.material.snackbar.Snackbar.make(findViewById(R.id.nav_host_fragment), "Saved to Gallery", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(R.id.nav_host_fragment), "Save failed", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
