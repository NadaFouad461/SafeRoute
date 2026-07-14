package com.example.saferoute.ui.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.data.repository.LocationRepository
import com.example.saferoute.databinding.FragmentSosBinding
import com.example.saferoute.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SosViewModel by viewModels()

    private lateinit var adapter: SosContactsAdapter

    private var passedSosAlertId: String? = null

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false

    // القيم دي بتتحدث فورًا بموقع GPS حقيقي في fetchCurrentLocation()، والقيم دي مجرد fallback أخير
    // لو مفيش أي موقع اتوصلنا بيه خالص (نادر جدًا لأن الصلاحيات بتتفحص الأول).
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var isRealLocationReady = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {

            if (PermissionManager.hasAllPermissions(requireContext())) {

                startSosCountdown()

            } else {

                Toast.makeText(
                    requireContext(),
                    "يجب إعطاء الصلاحيات أولاً",
                    Toast.LENGTH_LONG
                ).show()

                findNavController().popBackStack()

            }

        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSosBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSosBinding.bind(view)

        passedSosAlertId = arguments?.getString("sosAlertId")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fetchCurrentLocation()

        setupRecycler()

        observeViewModel()

        viewModel.loadCurrentUser()

        viewModel.loadEmergencyContacts()

        // بدء العد التنازلي تلقائياً عند الدخول للشاشة
        checkPermissions()

        binding.btnSos.setOnClickListener {
            if (!isTimerRunning) {
                checkPermissions()
            }
        }

        // إمكانية الضغط المطول لتفعيل الاستغاثة فوراً
        binding.btnSos.setOnLongClickListener {
            triggerSosImmediately()
            true
        }

        binding.btnCancelSos.setOnClickListener {
            cancelCountdown()
        }

        binding.btnOpenLogs.setOnClickListener {

            findNavController().navigate(
                R.id.historyFragment
            )

        }

    }

    private fun setupRecycler() {
        adapter = SosContactsAdapter(mutableListOf())
        adapter.onContactRemoved = { removedItems ->
            viewModel.removeContact(removedItems[0])
        }

        binding.rvEmergencyContactsHorizontal.adapter = adapter
    }

    private fun observeViewModel() {

        viewModel.contacts.observe(viewLifecycleOwner) {

            adapter.updateList(it)

        }
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSos.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) {

            if (it.isNotEmpty()) {

                Toast.makeText(
                    requireContext(),
                    it,
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

        viewModel.navigateToAlert.observe(viewLifecycleOwner) { alertId ->
            alertId ?: return@observe

            val bundle = Bundle().apply {
                putString("SOS_ALERT_ID", alertId)
            }

            findNavController().navigate(
                R.id.emergencyNotificationFragment,
                bundle
            )

            viewModel.clearNavigation()
        }

    }

    /**
     * بيجيب موقع حقيقي بدل الإحداثيات الثابتة القديمة.
     * أولاً بياخد آخر موقع محفوظ في LocationRepository لو موجود (فوري)، بعدين بيطلب
     * موقع طازج من FusedLocationProviderClient عشان يحدث القيمة لحظة إرسال الـ SOS.
     */
    private fun fetchCurrentLocation() {
        LocationRepository.getLastKnownLatLng()?.let { (lat, lon) ->
            currentLatitude = lat
            currentLongitude = lon
            isRealLocationReady = true
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        isRealLocationReady = true
                        LocationRepository.updateLocation(location.latitude, location.longitude)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SosFragment", "Failed to fetch location: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.e("SosFragment", "Location permission missing: ${e.message}")
        }
    }

    private fun checkPermissions() {

        if (
            PermissionManager.hasAllPermissions(
                requireContext()
            )
        ) {
            startSosCountdown()

        } else {

            permissionLauncher.launch(
                PermissionManager.REQUIRED_PERMISSIONS
            )

        }

    }

    private fun startSosCountdown() {
        if (isTimerRunning) return

        isTimerRunning = true
        binding.btnSos.visibility = View.VISIBLE
        binding.tvCountdown.visibility = View.VISIBLE
        binding.tvCountdown.text = "5"

        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000) + 1
                binding.tvCountdown.text = secondsLeft.toString()
            }

            override fun onFinish() {
                isTimerRunning = false
                triggerSosImmediately()
            }
        }.start()
    }

    private fun triggerSosImmediately() {
        countDownTimer?.cancel()
        isTimerRunning = false
        binding.tvCountdown.visibility = View.GONE
        binding.btnSos.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        if (!isRealLocationReady && _binding != null) {
            Toast.makeText(
                requireContext(),
                "⚠️ لسه مقدرناش نحدد موقعك بدقة، البلاغ هيتبعت بدون إحداثيات دقيقة!",
                Toast.LENGTH_LONG
            ).show()
        }

        val batteryManager =
            requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentBatteryLevel =
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        fetchLocationAndStartSos(currentBatteryLevel)
    }

    private fun fetchLocationAndStartSos(batteryLevel: Int) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val finalLat = location?.latitude ?: 30.0444
                val finalLon = location?.longitude ?: 31.2357

                if (location != null) LocationRepository.updateLocation(finalLat, finalLon)


                val message =
                    "🚨 استغاثة طارئة من SafeRoute!\nأحتاج للمساعدة، موقعي هو:\nخط عرض: $finalLat\nخط طول: $finalLon"

                val contacts = viewModel.getCurrentContacts()
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requireContext().getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                contacts.forEach { contact ->
                    if (contact.phone.isNotEmpty()) {
                        try {
                            val parts = smsManager?.divideMessage(message)
                            smsManager?.sendMultipartTextMessage(
                                contact.phone,
                                null,
                                parts,
                                null,
                                null
                            )
                            Log.d("SOS_SMS", "تم إرسال الموقع إلى: ${contact.phone}")
                        } catch (e: Exception) {
                            Log.e("SOS_SMS_ERROR", "فشل الإرسال إلى ${contact.phone}", e)
                        }
                    }
                }

                viewModel.startSos(finalLat, finalLon, passedSosAlertId, { alertId ->
                    val bundle = Bundle().apply {
                        putString("SOS_ALERT_ID", alertId)
                    }
                    findNavController().navigate(
                        R.id.emergencyNotificationFragment,
                        bundle
                    )
                }, batteryLevel) { errorMessage ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSos.visibility = View.VISIBLE
                    Toast.makeText(context, "خطأ: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val message =
                "🚨 استغاثة طارئة من SafeRoute! أحتاج للمساعدة، لا أستطيع مشاركة الموقع حالياً."
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requireContext().getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            viewModel.getCurrentContacts().forEach {
                try {
                    smsManager?.sendTextMessage(it.phone, null, message, null, null)
                } catch (e: Exception) {
                    Log.e("SOS_SMS_ERROR", "فشل إرسال رسالة الطوارئ", e)
                }
            }

            viewModel.startSos(30.0444, 31.2357, passedSosAlertId, { alertId ->
                val bundle = Bundle().apply {
                    putString("SOS_ALERT_ID", alertId)
                }
                findNavController().navigate(
                    R.id.emergencyNotificationFragment,
                    bundle
                )
            }, batteryLevel) { errorMessage ->
                binding.progressBar.visibility = View.GONE
                binding.btnSos.visibility = View.VISIBLE
                Toast.makeText(context, "خطأ: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        isTimerRunning = false
        binding.tvCountdown.visibility = View.GONE
        binding.btnSos.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE


        if (!passedSosAlertId.isNullOrEmpty()) {
            viewModel.cancelSOS(passedSosAlertId!!)
            Toast.makeText(context, "تم إلغاء الاستغاثة", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تم إلغاء العد التنازلي", Toast.LENGTH_SHORT).show()
        }
        findNavController().popBackStack()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }
}
