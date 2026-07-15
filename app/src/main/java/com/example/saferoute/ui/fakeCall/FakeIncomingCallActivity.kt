package com.example.saferoute.ui.fakeCall

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.saferoute.databinding.ActivityFakeIncomingCallBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class FakeIncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeIncomingCallBinding
    private var ringtone: Ringtone? = null
    private var callHandler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private var isCallActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFakeIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make activity show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"
        binding.tvCallerName.text = callerName
        binding.tvAvatarText.text = if (callerName.isNotEmpty()) callerName[0].toString().uppercase() else "?"

        playRingtone()
        setupButtons()
    }

    private fun playRingtone() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        binding.fabDecline.setOnClickListener {
            stopCall()
        }

        binding.fabAccept.setOnClickListener {
            acceptCall()
        }

        binding.fabHangUp.setOnClickListener {
            stopCall()
        }
    }

    private fun acceptCall() {
        isCallActive = true
        ringtone?.stop()
        binding.llActions.visibility = View.GONE
        binding.llOngoingCall.visibility = View.VISIBLE
        binding.tvCallStatus.text = "On Call..."
        startTimer()
    }

    private fun startTimer() {
        callHandler.post(object : Runnable {
            override fun run() {
                if (isCallActive) {
                    seconds++
                    val mins = seconds / 60
                    val secs = seconds % 60
                    binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                    callHandler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopCall() {
        isCallActive = false
        ringtone?.stop()
        callHandler.removeCallbacksAndMessages(null)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        callHandler.removeCallbacksAndMessages(null)
    }
}
