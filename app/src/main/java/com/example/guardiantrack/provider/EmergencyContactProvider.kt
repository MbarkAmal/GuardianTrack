package com.example.guardiantrack.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.example.guardiantrack.data.model.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.guardiantrack.data.model.EmergencyContactDao

/**
 * ContentProvider exposing emergency contacts stored in Room.
 * 
 * URI: content://com.guardian.track.provider/emergency_contacts
 * 
 * Protected by the custom permission:
 *   com.guardian.track.READ_EMERGENCY_CONTACTS (protectionLevel=signature|privileged)
 * 
 * Security choice rationale:
 *   - "signature|privileged" ensures ONLY apps signed with the same certificate
 *     (or pre-installed system apps) can access the emergency contact data,
 *     protecting sensitive personal safety information from 3rd-party apps.
 */
class EmergencyContactProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.guardian.track.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/emergency_contacts")

        private const val EMERGENCY_CONTACTS = 1

        val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "emergency_contacts", EMERGENCY_CONTACTS)
        }

        // ContentProvider column names
        const val COL_ID         = "_id"
        const val COL_NAME       = "name"
        const val COL_PHONE      = "phone_number"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EmergencyContactProviderEntryPoint {
        fun emergencyContactDao(): EmergencyContactDao
    }

    private fun getDao(): EmergencyContactDao {
        val appContext = context?.applicationContext ?: throw IllegalStateException("Context is null")
        val entryPoint = EntryPoints.get(appContext, EmergencyContactProviderEntryPoint::class.java)
        return entryPoint.emergencyContactDao()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (uriMatcher.match(uri) != EMERGENCY_CONTACTS) {
            throw IllegalArgumentException("Unknown URI: $uri")
        }

        val cursor = MatrixCursor(arrayOf(COL_ID, COL_NAME, COL_PHONE))

        // Run synchronously in the ContentProvider (Room query on background thread handled by caller)
        val dao = getDao()
        // Use runBlocking only because ContentProvider.query() must be synchronous
        val contacts = kotlinx.coroutines.runBlocking { 
            // getAllContacts is a Flow - collect first element
            var result = listOf<com.example.guardiantrack.data.model.EmergencyContactEntity>()
            dao.getAllContacts().collect { result = it }
            result
        }

        for (contact in contacts) {
            cursor.addRow(arrayOf(contact.id, contact.name, contact.phoneNumber))
        }

        cursor.setNotificationUri(context?.contentResolver, CONTENT_URI)
        return cursor
    }

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        EMERGENCY_CONTACTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.emergency_contacts"
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    // Read-only provider: insert/update/delete not supported
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
