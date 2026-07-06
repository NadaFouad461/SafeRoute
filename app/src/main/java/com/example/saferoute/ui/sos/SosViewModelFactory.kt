package com.example.saferoute.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.saferoute.data.repository.SosRepository

class SosViewModelFactory(
    private val repository: SosRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        return SosViewModel(repository) as T
    }
}