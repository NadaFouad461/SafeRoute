package com.example.saferoute.ui.sos

import androidx.lifecycle.*
import com.example.saferoute.data.repository.SosRepository

class SosViewModel(
    private val repository: SosRepository
) : ViewModel() {


    private val _sosStatus = MutableLiveData<SosState>()
    val sosStatus: LiveData<SosState> = _sosStatus
}

sealed class SosState {
    object Loading : SosState()
    object Success : SosState()
    data class Error(val message: String) : SosState()
}