package com.bunkerstock.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bunkerstock.app.AndroidBunkerPlatform
import com.bunkerstock.app.data.BunkerRepository

class BunkerViewModel(application: Application) : AndroidViewModel(application) {
    val repository = BunkerRepository(application.applicationContext)
    val platform = AndroidBunkerPlatform(application, repository, viewModelScope)
    val controller = BunkerController(viewModelScope, repository, platform)
}
