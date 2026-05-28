package com.example.smatchup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smatchup.SmatchupApp
import com.example.smatchup.di.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        else -> error("Unknown ViewModel ${modelClass.name}")
    }

    companion object {
        fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
    }
}
