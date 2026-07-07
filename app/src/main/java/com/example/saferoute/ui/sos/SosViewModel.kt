package com.example.saferoute.ui.sos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.saferoute.data.repository.SosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
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