package com.example.guardiantrack.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.guardiantrack.R
import com.example.guardiantrack.databinding.FragmentDashboardBinding
import com.example.guardiantrack.receiver.BatteryReceiver
import com.example.guardiantrack.viewmodel.IncidentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.monitoringState.observe(viewLifecycleOwner) { state ->
            binding.tvBatteryStatus.text = "Battery: ${state.batteryLevel}%"
            binding.tvAccelerometerStatus.text = "Accelerometer: ${if (state.isAccelerometerActive) "OK" else "Error"}"
            binding.tvGpsStatus.text = "GPS: ${state.gpsStatus}"
            
            state.lastIncidentType?.let {
                // Optional: Show a UI indicator if a specific incident just occurred
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        binding.btnEmergencyAlert.setOnClickListener {
            viewModel.sendEmergencyAlert()
            Toast.makeText(requireContext(), "🆘 SOS ALERT SENT!", Toast.LENGTH_LONG).show()
        }

        // DEBUG ONLY — simulates battery low directly on the device
        binding.btnTestBatteryLow.setOnClickListener {
            val fakeIntent = Intent(Intent.ACTION_BATTERY_LOW)
            BatteryReceiver().onReceive(requireContext(), fakeIntent)
            Toast.makeText(requireContext(), "✅ Battery Low simulé !", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}