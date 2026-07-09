package com.example.saferoute.ui.sensors


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentSafeWalkBinding
import com.example.saferoute.services.SafeWalkService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SafeWalkFragment : Fragment(R.layout.fragment_safe_walk) {

    private var _binding: FragmentSafeWalkBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSafeWalkBinding.bind(view)


        if (!SafeWalkService.isWalkActive.value) {
            val serviceIntent = Intent(requireContext(), SafeWalkService::class.java).apply {
                putExtra("DURATION_MINUTES", 1)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
        }

        // 2. مراقبة العداد التنازلي وتحديث الشاشة (Real-time)
        viewLifecycleOwner.lifecycleScope.launch {
            SafeWalkService.timeRemaining.collect { timeString ->
                binding.tvTripDuration.text = timeString
            }
        }

        setupButtons()

        // 💡 ملاحظة لزميلك بتاع الخرائط: 
        // كود تشغيل الـ osmdroid للـ miniMap هيتكتب هنا لاحقاً
    }

    private fun setupButtons() {
        // أ. المستخدم وصل بالسلامة أو عايز ينهي الرحلة
        val endTripAction = View.OnClickListener {
            stopServiceAndExit(triggerSOS = false)
        }
        binding.btnArrivedSafely.setOnClickListener(endTripAction)
        binding.btnEndTrip.setOnClickListener(endTripAction)

        // ب. زرار الطوارئ الفوري (لو حس بخطر وعايز يضرب الإنذار فوراً)
        binding.cardFeelingUnsafe.setOnClickListener {
            stopServiceAndExit(triggerSOS = true)
        }
    }

    private fun stopServiceAndExit(triggerSOS: Boolean) {
        // 1. نوقف الخدمة والعداد
        requireContext().stopService(Intent(requireContext(), SafeWalkService::class.java))

        // 2. نحدد الخطوة الجاية
        if (triggerSOS) {
            // نفتح شاشة الإنذار الأحمر
            val intent = Intent(requireContext(), FallAlertActivity::class.java)
            startActivity(intent)
        } else {
            // نرجع للشاشة الرئيسية (الهوم)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}