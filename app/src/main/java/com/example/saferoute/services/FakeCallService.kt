package com.example.saferoute.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.saferoute.ui.fakeCall.FakeIncomingCallActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FakeCallService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // بنستقبل اسم المتصل اللي حددناه
        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"

        // Trigger the fake call screen
        val callIntent = Intent(context, FakeIncomingCallActivity::class.java).apply {
            putExtra("CALLER_NAME", callerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(callIntent)
    }
}
