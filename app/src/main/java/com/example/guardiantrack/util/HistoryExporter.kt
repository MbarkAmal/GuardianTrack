package com.example.guardiantrack.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.guardiantrack.data.model.IncidentEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryExporter {

    fun exportToCsv(context: Context, incidents: List<IncidentEntity>): Boolean {
        val fileName = "GuardianTrack_History_${System.currentTimeMillis()}.csv"
        val header = "ID;Date;Time;Type;Latitude;Longitude;Address;Sync Status\n"
        val data = buildString {
            append(header)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            
            for (incident in incidents) {
                val dateStr = dateFormat.format(Date(incident.timestamp))
                val timeStr = timeFormat.format(Date(incident.timestamp))
                val syncStatus = if (incident.isSynced) "Synced" else "Pending"
                append("${incident.id};$dateStr;$timeStr;${incident.type};${incident.latitude};${incident.longitude};${incident.address ?: ""};$syncStatus\n")
            }
        }
        return saveFileToScopedStorage(context, fileName, "text/csv", data)
    }

    fun exportToTxt(context: Context, incidents: List<IncidentEntity>): Boolean {
        val fileName = "GuardianTrack_History_${System.currentTimeMillis()}.txt"
        val data = buildString {
            append("--- GuardianTrack Incident History ---\n\n")
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            
            for (incident in incidents) {
                val dateStr = dateTimeFormat.format(Date(incident.timestamp))
                val syncStatus = if (incident.isSynced) "Synced" else "Pending"
                append("ID: ${incident.id}\n")
                append("Date: $dateStr\n")
                append("Type: ${incident.type}\n")
                append("Location: ${incident.latitude}, ${incident.longitude}\n")
                append("Address: ${incident.address ?: "N/A"}\n")
                append("Status: $syncStatus\n")
                append("------------------------------------\n")
            }
        }
        return saveFileToScopedStorage(context, fileName, "text/plain", data)
    }

    private fun saveFileToScopedStorage(context: Context, fileName: String, mimeType: String, content: String): Boolean {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GuardianTrack")
            }
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues) 
            ?: return false

        return try {
            val outputStream: OutputStream? = resolver.openOutputStream(uri)
            outputStream?.use {
                it.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
