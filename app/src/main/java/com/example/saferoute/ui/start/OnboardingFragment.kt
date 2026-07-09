package com.example.saferoute.ui.start

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {


    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)


        val onboardingAdapter = OnboardingAdapter(
            onSosClick = {

                binding.viewPager.setCurrentItem(1, true)
            },
            onLocationClick = {

                binding.viewPager.setCurrentItem(2, true)
            },
            onFinalGetStartedClick = {

                findNavController().navigate(R.id.action_onboardingFragment_to_permissionsFragment)
            }
        )

        binding.viewPager.adapter = onboardingAdapter


        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPager) { _, _ ->

        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}