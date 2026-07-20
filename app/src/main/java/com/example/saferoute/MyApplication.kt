package com.example.saferoute

import android.app.Application
import android.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
    }

    /**
     * OSM's free tile servers block requests whose User-Agent is the default
     * "com.example.*" package name, since that's the generic Android Studio
     * template used by thousands of tutorial apps and gets flagged as abuse
     * (this is what caused the "403 Access blocked / not following tile
     * usage policy" screens). We set a unique, identifiable User-Agent here
     * ONCE for the whole app, instead of repeating it (with the wrong value)
     * in every Activity/Fragment that shows a map.
     */
    private fun configureOsmdroid() {
        val config = Configuration.getInstance()
        config.load(this, PreferenceManager.getDefaultSharedPreferences(this))
        config.userAgentValue = "SafeRoute-DEPI-App/1.0 (+https://github.com/AbdulrhmanMustafa-dev/SafeRoute)"
        config.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }
}