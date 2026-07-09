package com.example.saferoute.ui.start

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            routeUser()
        }
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    fun routeUser() {
        val sharedPrefs =
            requireActivity().getSharedPreferences("SafeRoutePrefs", Context.MODE_PRIVATE)
        val isFirstTime = sharedPrefs.getBoolean("isFirstTime", true)

        if (isFirstTime) {
            // 1. أول مرة يفتح التطبيق -> وديه للـ Onboarding
            findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)
        } else {
            // 2. مش أول مرة -> نشيك لو عامل تسجيل دخول
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // 🟢 مسجل دخول -> وديه للـ Home مباشرة
                findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
            } else {
                // 🔴 مش مسجل دخول -> وديه للـ Login
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }
    }
}
