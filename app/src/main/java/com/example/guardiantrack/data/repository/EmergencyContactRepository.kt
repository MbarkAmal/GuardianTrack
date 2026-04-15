package com.example.guardiantrack.data.repository

import com.example.guardiantrack.data.model.EmergencyContactDao
import com.example.guardiantrack.data.model.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactRepository @Inject constructor(
    private val contactDao: EmergencyContactDao
) {
    fun getAllContacts(): Flow<List<EmergencyContactEntity>> = contactDao.getAllContacts()

    suspend fun insertContact(contact: EmergencyContactEntity) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: EmergencyContactEntity) {
        contactDao.deleteContact(contact)
    }

    suspend fun isNumberDuplicate(number: String): Boolean {
        return contactDao.exists(number)
    }
}
