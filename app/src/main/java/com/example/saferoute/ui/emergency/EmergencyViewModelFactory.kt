package com.example.saferoute.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.saferoute.data.repository.EmergencyRepository

class EmergencyViewModelFactory(
    private val repository: EmergencyRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EmergencyViewModel(repository) as T
    }
}