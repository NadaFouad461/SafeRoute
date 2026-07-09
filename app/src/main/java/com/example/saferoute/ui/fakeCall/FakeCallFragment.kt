package com.example.saferoute.ui.fakeCall


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentFakeCallBinding
import com.example.saferoute.services.FakeCallService
import dagger.hilt.android.AndroidEntryPoint
import kotlin.jvm.java

@AndroidEntryPoint
class FakeCallFragment : Fragment(R.layout.fragment_fake_call) {

    private var _binding: FragmentFakeCallBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFakeCallBinding.bind(view)


        setupSimulationButton()
    }

    private fun setupSimulationButton() {
        binding.btnStartSimulation.setOnClickListener {
            val callerName = binding.etCallerName.text.toString().trim()
            if (callerName.isEmpty()) {
                Toast.makeText(requireContext(), "برجاء تحديد اسم المتصل", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 🚨 الفحص الجديد لأندرويد 12 وأعلى: هل معانا صلاحية المنبه الدقيق؟
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(requireContext(), "برجاء تفعيل إذن 'المنبهات الدقيقة' لضمان وصول المكالمة", Toast.LENGTH_LONG).show()
                // نودي المستخدم لشاشة الإعدادات الخاصة بالتطبيق عشان يفعلها
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return@setOnClickListener
            }

            // حساب الوقت بالمللي ثانية
            val delayMillis = when (binding.rgDelay.checkedRadioButtonId) {
                R.id.rbDelay5s -> 5000L
                R.id.rbDelay10s -> 10000L
                R.id.rbDelay30s -> 30000L
                R.id.rbDelay1m -> 60000L
                else -> 5000L
            }

            val intent = Intent(requireContext(),  FakeCallService::class.java).apply {
                putExtra("CALLER_NAME", callerName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + delayMillis
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

            Toast.makeText(requireContext(), "تم الجدولة! سيتصل بك بعد ${delayMillis / 1000} ثواني", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}