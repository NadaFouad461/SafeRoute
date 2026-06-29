package com.example.saferoute.ui.start

import PermissionModel
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentPermissionsBinding


class PermissionsFragment : Fragment(R.layout.fragment_permissions) {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPermissionsBinding.bind(view)


        val permissionList = listOf(
            PermissionModel(
                title = "Precise Location",
                description = "Required for real-time SOS tracking and sharing your live route with emergency contacts.",
                iconRes = android.R.drawable.ic_menu_mylocation,
                bannerRes = R.drawable.permission_location,
                manifestPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PermissionModel(
                title = "Emergency SMS",
                description = "Allows SafeRoute to automatically send distress messages to your contacts when SOS is triggered.",
                iconRes = R.drawable.permission_emergency_sms,
                bannerRes = R.drawable.boarding_sos,
                manifestPermission = android.Manifest.permission.SEND_SMS
            ),
            PermissionModel(
                title = "Critical Alerts",
                description = "Receive vital safety check-ins, fall detection warnings, and nearby danger zone alerts.",
                iconRes = android.R.drawable.ic_popup_reminder,
                bannerRes = R.drawable.permission_alert,
                manifestPermission = android.Manifest.permission.POST_NOTIFICATIONS
            )
        )


        val adapter = PermissionsAdapter(permissionList) { item, isChecked ->
            item.isGranted = isChecked


        }


        binding.rvPermissions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPermissions.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}