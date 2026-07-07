package com.example.saferoute.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.saferoute.ui.sensors.FallAlertActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FakeCallService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // بنستقبل اسم المتصل اللي حددناه
        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"

        // 💡 تقدر تغير FakeIncomingCallActivity لـ SosActivity لو عايز تفتح الـ SOS
        val callIntent = Intent(context, FallAlertActivity::class.java).apply {
            putExtra("CALLER_NAME", callerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(callIntent)
    }
}