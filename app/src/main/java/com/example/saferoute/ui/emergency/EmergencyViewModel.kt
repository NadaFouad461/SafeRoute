package com.example.saferoute.ui.emergency

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferoute.data.local.EmergencyLog
import com.example.saferoute.data.repository.EmergencyRepository
import kotlinx.coroutines.launch

class EmergencyViewModel(
    private val repository: EmergencyRepository
) : ViewModel() {

    val logs: LiveData<List<EmergencyLog>> =
        repository.getAllLogs()


    fun saveLog(log: EmergencyLog) {
        viewModelScope.launch {
            repository.insertLog(log)
        }
    }
}