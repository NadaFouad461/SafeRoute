package com.example.saferoute.ui.sensors


import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.saferoute.databinding.ActivityFallAlertBinding
import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class FallAlertActivity: AppCompatActivity() {

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
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        binding = ActivityFallAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                // الـ Countdown خلص ومفيش رد → روح لـ SOS
                goToSOS()
            }

        }.start()
    }

    private fun setupButtons() {
        // المستخدم بخير
        binding.btnImOkay.setOnClickListener {
            countDownTimer?.cancel()
            finish() // أغلق الـ screen وارجع عادي
        }
    }

    private fun goToSOS() {
        // هنا هتنسق مع عضو الـ SOS في التيم
        // دلوقتي بس بنروح لـ SOSFragment
        finish()
        // لو عندك SOSActivity:
        // startActivity(Intent(this, SOSActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}