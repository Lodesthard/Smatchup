package com.example.smatchup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smatchup.SmatchupApp
import com.example.smatchup.di.AppContainer
import com.example.smatchup.ui.character.CharacterListViewModel
import com.example.smatchup.ui.home.HomeViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java          -> HomeViewModel() as T
        CharacterListViewModel::class.java -> CharacterListViewModel(container.characterRepository) as T
        else -> error("Unknown ViewModel ${modelClass.name}")
    }

    companion object {
        fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
    }
}
