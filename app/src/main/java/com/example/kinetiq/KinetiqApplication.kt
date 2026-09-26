package com.example.kinetiq

import android.app.Application
import com.example.kinetiq.data.local.AppDatabase
import com.example.kinetiq.data.repository.AppRepository

class KinetiqApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: AppRepository by lazy { AppRepository(database) }

    override fun onCreate() {
        super.onCreate()
    }
}