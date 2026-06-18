package com.example.saferoute.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.saferoute.R
import com.example.saferoute.databinding.FragmentSensorBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SensorFragment : Fragment(R.layout.fragment_sensor) {

    private var _binding: FragmentSensorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SensorViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.switchFallDetection.isChecked = true
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchFallDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.startDetection()
            else viewModel.stopDetection()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}