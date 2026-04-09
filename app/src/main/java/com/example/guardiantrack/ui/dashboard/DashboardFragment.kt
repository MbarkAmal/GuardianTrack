package com.example.guardiantrack.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.guardiantrack.R
import com.example.guardiantrack.databinding.FragmentDashboardBinding
import com.example.guardiantrack.receiver.BatteryReceiver

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
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