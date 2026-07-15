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

import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.saferoute.models.ContactItem
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@AndroidEntryPoint
class FakeCallFragment : Fragment(R.layout.fragment_fake_call) {

    private var _binding: FragmentFakeCallBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFakeCallBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupSimulationButton()
        setupTextWatcher()
        setupPresetChips()
        loadEmergencyContacts()
    }

    private fun setupTextWatcher() {
        binding.etCallerName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updatePreview(name: String) {
        val displayName = if (name.isEmpty()) "Unknown" else name
        binding.tvPreviewName.text = displayName
        binding.tvPreviewAvatar.text = if (displayName.isNotEmpty()) displayName[0].toString().uppercase() else "📞"
    }

    private fun setupPresetChips() {
        binding.chipMom.setOnClickListener { binding.etCallerName.setText("Mom") }
        binding.chipBoss.setOnClickListener { binding.etCallerName.setText("Boss") }
        binding.chipDelivery.setOnClickListener { binding.etCallerName.setText("Pizza Delivery") }
    }

    private fun loadEmergencyContacts() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("contacts")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val contact = document.toObject(ContactItem::class.java)
                    addContactChip(contact)
                }
            }
    }

    private fun addContactChip(contact: ContactItem) {
        val chip = Chip(requireContext()).apply {
            text = "👤 ${contact.name}"
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            chipStrokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E2E8F0"))
            chipStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
            setOnClickListener {
                binding.etCallerName.setText(contact.name)
            }
        }
        
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 24, 0) // Spacing between chips
        chip.layoutParams = params
        
        binding.chipGroup.addView(chip)
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
