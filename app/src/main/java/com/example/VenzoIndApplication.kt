package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class VenzoIndApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Initialize Firestore instance for persistent real-time chat data storage
        try {
            com.example.data.sync.FirestoreManager.getInstance(this)
        } catch (e: Exception) {
            android.util.Log.e("VenzoIndApp", "Firestore init: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .allowHardware(true)
            .allowRgb565(false)
            .crossfade(true)
            .build()
    }
}
