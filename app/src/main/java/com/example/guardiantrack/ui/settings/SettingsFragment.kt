package com.example.guardiantrack.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guardiantrack.databinding.FragmentSettingsBinding
import com.example.guardiantrack.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var contactAdapter: EmergencyContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupContactsList()
        setupSensitivitySlider()
        observeViewModel()
        setupSaveButton()
    }

    private fun setupSensitivitySlider() {
        binding.sbSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Slider is 0-15. Map to 10-25 m/s²
                val threshold = progress + 10
                binding.tvSensitivityValue.text = "$threshold m/s²"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupContactsList() {
        contactAdapter = EmergencyContactAdapter { contact ->
            viewModel.deleteContact(contact)
        }
        binding.rvEmergencyContacts.apply {
            adapter = contactAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnAddContactBtn.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(com.example.guardiantrack.R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(com.example.guardiantrack.R.id.etDialigContactName)
        val etPhone = dialogView.findViewById<EditText>(com.example.guardiantrack.R.id.etDialigContactPhone)

        AlertDialog.Builder(requireContext())
            .setTitle("Ajouter un contact d'urgence")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (!phone.matches(Regex("^\\+?[0-9]{6,15}$"))) {
                    Toast.makeText(requireContext(), "Numéro invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addContact(name, phone)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Dark mode
                launch {
                    viewModel.darkModeActive.collect { isActive ->
                        binding.switchDarkMode.isChecked = isActive
                    }
                }

                // Sensitivity Threshold
                launch {
                    viewModel.sensitivityThreshold.collect { threshold ->
                        // Reverse mapping: 10-25 -> 0-15
                        val progress = (threshold - 10).toInt().coerceIn(0, 15)
                        binding.sbSensitivity.progress = progress
                        binding.tvSensitivityValue.text = "${threshold.toInt()} m/s²"
                    }
                }

                // Contacts List
                launch {
                    viewModel.contacts.collect { contactsList ->
                        contactAdapter.submitList(contactsList)
                    }
                }

                // Add Contact Result (including duplicate check)
                launch {
                    viewModel.addContactResult.collect { result ->
                        result?.let {
                            if (it.isSuccess) {
                                Toast.makeText(requireContext(), "Contact ajouté", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Erreur : ${it.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                            viewModel.resetAddContactResult()
                        }
                    }
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveSettings.setOnClickListener {
            val darkMode = binding.switchDarkMode.isChecked
            val threshold = (binding.sbSensitivity.progress + 10).toFloat()

            viewModel.saveSettings(darkMode, threshold)

            // Apply dark mode immediately
            val mode = if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
                       else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)

            Toast.makeText(requireContext(), "✅ Paramètres enregistrés", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

