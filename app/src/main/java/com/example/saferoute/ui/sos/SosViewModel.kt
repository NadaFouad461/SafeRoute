package com.example.saferoute.ui.sos

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import com.example.saferoute.data.repository.EmergencyRepository
import com.example.saferoute.models.ContactItem
import com.google.firebase.auth.FirebaseAuth
import android.telephony.SmsManager
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.saferoute.data.repository.SosRepository

@HiltViewModel
class SosViewModel @Inject constructor(
    private val repository: EmergencyRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUserId = auth.currentUser?.uid ?: "unknown_user"



    private val _contacts = MutableLiveData<MutableList<ContactItem>>(mutableListOf())
    val contacts: LiveData<MutableList<ContactItem>> get() = _contacts

    private val _userName = MutableLiveData("مستخدم")
    val userName: LiveData<String> get() = _userName

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _navigateToAlert = MutableLiveData<String?>()
    val navigateToAlert: LiveData<String?> get() = _navigateToAlert

    fun clearNavigation() {
        _navigateToAlert.value = null
    }

    fun loadCurrentUser() {
        if (currentUserId == "unknown_user") {
            _error.value = "User not found"
            return
        }
        repository.getUserData(currentUserId) { success, data, message ->
            if (success && data != null) {
                _userName.value = data["name"]?.toString() ?: "مستخدم"
            } else {
                _error.value = message ?: "Failed loading user"
            }
        }
    }

    fun loadEmergencyContacts() {
        if (currentUserId == "unknown_user") {
            _error.value = "لم يتم العثور على المستخدم"
            return
        }
        _loading.value = true
        repository.loadEmergencyContacts(
            currentUserId,
            onSuccess = { list -> _loading.value = false; _contacts.value = list },
            onFailure = { message -> _loading.value = false; _error.value = message }
        )
    }



    fun getCurrentContacts(): MutableList<ContactItem> {

        return _contacts.value ?: mutableListOf()

    }

    fun getCurrentUserName(): String {

        return _userName.value ?: "مستخدم"

    }
    fun startSos(
        currentLatitude: Double,
        currentLongitude: Double,
        passedSosAlertId: String?,
        onFinished: (String) -> Unit,
        batteryLevel: Int,
        onError: (String) -> Unit
    ) {
        val contacts = getCurrentContacts()
        if (contacts.isEmpty()) {
            onError("يرجى الانتظار حتى يتم تحميل جهات اتصال الطوارئ.")
            return
        }

        val sharedWith = contacts.map { it.userUid }.filter { it.isNotEmpty() }

        val emergencyData = hashMapOf<String, Any>(
            "userId" to currentUserId,
            "userName" to getCurrentUserName(),
            "status" to "Emergency Dispatched",
            "type" to "SOS",
            "latitude" to currentLatitude,
            "longitude" to currentLongitude,
            "timestamp" to Timestamp.now(),
            "sharedWith" to sharedWith,
            "batteryLevel" to batteryLevel
        )

        if (!passedSosAlertId.isNullOrEmpty()) {
            repository.updateEmergency(
                documentId = passedSosAlertId,
                updateData = emergencyData,
                onSuccess = {
                    contacts.forEach { if (it.fcmToken.isNotEmpty()) sendFcmNotification(it.fcmToken, getCurrentUserName(), passedSosAlertId) }
                    onFinished(passedSosAlertId)
                },
                onFailure = { onError(it) }
            )
        } else {
            repository.createEmergency(
                emergencyData = emergencyData,
                onSuccess = { documentId ->
                    contacts.forEach { if (it.fcmToken.isNotEmpty()) sendFcmNotification(it.fcmToken, getCurrentUserName(), documentId) }
                    onFinished(documentId)
                },
                onFailure = { onError(it) }
            )
        }
    }
    private fun sendFcmNotification(

        token: String,
        senderName: String,
        alertId: String

    ) {

        val dataPayload = hashMapOf(

            "title" to "🚨 استغاثة طارئة من $senderName",

            "body" to "الرجاء المساعدة، تم فتح بلاغ طوارئ نشط الآن!",

            "SOS_ALERT_ID" to alertId,

            "senderName" to senderName,

            "click_action" to "EMERGENCY_NOTIFICATION"

        )

        val notificationPayload = hashMapOf(

            "title" to "🚨 استغاثة طارئة من $senderName",

            "body" to "الرجاء المساعدة، تم فتح بلاغ طوارئ نشط الآن!"

        )

        val notification = hashMapOf<String, Any>(

            "token" to token,

            "to" to token,

            "priority" to "high",

            "notification" to notificationPayload,

            "data" to dataPayload

        )

        repository.queueNotification(notification)

    }
    fun cancelSOS(

        alertId: String?

    ) {

        if (alertId.isNullOrEmpty()) return

        repository.cancelEmergency(alertId)

    }
    fun removeContact(contactToRemove: ContactItem) {
        val currentList = _contacts.value ?: mutableListOf()
        currentList.remove(contactToRemove)
        _contacts.value = currentList
    }
    fun updateContactsList(newList: MutableList<ContactItem>) {
        _contacts.value = newList
    }

}
