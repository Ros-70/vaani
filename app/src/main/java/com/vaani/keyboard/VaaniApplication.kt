package com.vaani.keyboard

import android.app.Application
import com.vaani.keyboard.data.VaaniRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VaaniApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: VaaniRepository

    override fun onCreate() {
        super.onCreate()
        repository = VaaniRepository(this)
        applicationScope.launch { repository.seedDefaultsIfEmpty() }
    }
}
