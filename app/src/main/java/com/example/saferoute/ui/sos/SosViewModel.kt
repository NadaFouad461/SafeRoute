package com.example.saferoute.ui.sos

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferoute.data.repository.SosRepository
import kotlinx.coroutines.launch

class SosViewModel(
    private val repository: SosRepository
) : ViewModel() {

    private val _sosStatus = MutableLiveData<SosState>()
    val sosStatus: LiveData<SosState> = _sosStatus

    fun triggerSos(
        context: Context,
        userId: String,
        emergencyNumbers: List<String>
    ) {

        _sosStatus.value = SosState.Loading

        viewModelScope.launch {

            android.util.Log.d("SOS_DEBUG", "Trigger SOS called")

            val result = repository.sendEmergencySos(
                context,
                userId,
                emergencyNumbers
            )

            result.onSuccess {

                _sosStatus.postValue(
                    SosState.Success
                )

            }.onFailure { exception ->

                _sosStatus.postValue(
                    SosState.Error(
                        exception.message
                            ?: "حدث خطأ غير معروف"
                    )
                )
            }
        }
    }
}

sealed class SosState {

    object Loading : SosState()

    object Success : SosState()

    data class Error(
        val message: String
    ) : SosState()
}