package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class VenzoIndApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase App Check with Play Integrity (Production) & Debug Provider (Development)
        initFirebaseAppCheck()

        // 2. Initialize Firestore instance for persistent real-time chat data storage
        try {
            com.example.data.sync.FirestoreManager.getInstance(this)
        } catch (e: Exception) {
            Log.e("VenzoIndApp", "Firestore init: ${e.message}")
        }
    }

    /**
     * Initializes Firebase App Check with Play Integrity Provider for production
     * to protect backend services, Firestore, and APIs from unauthorized abuse.
     */
    private fun initFirebaseAppCheck() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                // Debug builds: Use DebugAppCheckProviderFactory for local development and testing
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d("VenzoIndApp", "Firebase App Check initialized: DebugAppCheckProviderFactory")
            } else {
                // Release/Production builds: Use PlayIntegrityAppCheckProviderFactory for Play Integrity attestation
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d("VenzoIndApp", "Firebase App Check initialized: PlayIntegrityAppCheckProviderFactory")
            }
        } catch (e: Exception) {
            Log.e("VenzoIndApp", "Firebase App Check initialization failed: ${e.message}")
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
