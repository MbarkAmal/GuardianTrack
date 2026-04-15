package com.example.guardiantrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardiantrack.data.PreferenceManager
import com.example.guardiantrack.data.model.EmergencyContactEntity
import com.example.guardiantrack.data.repository.EmergencyContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val contactRepository: EmergencyContactRepository
) : ViewModel() {

    // UI states derived from DataStore
    val darkModeActive = preferenceManager.darkModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Save dark mode setting
    fun saveSettings(darkMode: Boolean) {
        viewModelScope.launch {
            preferenceManager.saveDarkMode(darkMode)
        }
    }

    // --- Contacts Management ---
    
    val contacts = contactRepository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addContactResult = MutableStateFlow<Result<Unit>?>(null)
    val addContactResult: StateFlow<Result<Unit>?> = _addContactResult

    fun addContact(name: String, phone: String) {
        viewModelScope.launch {
            try {
                if (contactRepository.isNumberDuplicate(phone)) {
                    _addContactResult.value = Result.failure(Exception("Ce numéro existe déjà."))
                } else {
                    val newContact = EmergencyContactEntity(name = name, phoneNumber = phone)
                    contactRepository.insertContact(newContact)
                    _addContactResult.value = Result.success(Unit)
                }
            } catch (e: Exception) {
                _addContactResult.value = Result.failure(e)
            }
        }
    }

    fun resetAddContactResult() {
        _addContactResult.value = null
    }

    fun deleteContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
        }
    }
}

