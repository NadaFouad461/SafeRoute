package com.example.saferoute.ui.sensors


import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.saferoute.databinding.ActivityFallAlertBinding
import com.example.saferoute.services.FallDetectionService
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FallAlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFallAlertBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFallAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {

            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        startCountdown()
        setupButtons()
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(10_000, 1_000) {

            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                binding.tvCountdown.text = secondsLeft.toString()
            }

            override fun onFinish() {
                goToSOS()
            }

        }.start()
    }

    private fun setupButtons() {
        binding.btnCancelSos.setOnClickListener {
            countDownTimer?.cancel()
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(FallDetectionService.FALL_ALERT_NOTIFICATION_ID)
            finish()
        }
    }

    private fun goToSOS() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}