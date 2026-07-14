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

    fun View.pressAnimation(action: () -> Unit) {
        animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(80)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .withEndAction(action)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        val onboardingAdapter = OnboardingAdapter()
        binding.viewPager.adapter = onboardingAdapter

        // Initial state
        updateButtons(0, onboardingAdapter.itemCount)

        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtons(position, onboardingAdapter.itemCount)
            }
        })

        binding.btnNext.setOnClickListener {
            it.pressAnimation {
                val current = binding.viewPager.currentItem
                val count = onboardingAdapter.itemCount
                if (current < count - 1) {
                    binding.viewPager.setCurrentItem(current + 1, true)
                } else {
                    findNavController().navigate(R.id.action_onboardingFragment_to_permissionsFragment)
                }
            }
        }

        binding.btnBack.setOnClickListener {
            it.pressAnimation {
                val current = binding.viewPager.currentItem
                if (current > 0) {
                    binding.viewPager.setCurrentItem(current - 1, true)
                }
            }
        }

        binding.btnSkip.setOnClickListener {
            it.pressAnimation {
                findNavController().navigate(R.id.action_onboardingFragment_to_permissionsFragment)
            }
        }

        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPager) { _, _ -> }.attach()
    }

    private fun updateButtons(position: Int, itemCount: Int) {
        // Hide back button on first page
        binding.btnBack.visibility = if (position == 0) View.GONE else View.VISIBLE

        // Hide skip button on last page
        binding.btnSkip.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE

        // Change next button text on last page
        binding.btnNext.text = if (position == itemCount - 1) "Get Started" else "Next"
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
