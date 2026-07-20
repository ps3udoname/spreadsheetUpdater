package com.example.spreadsheetupdater

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.work.*
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ApiCaller(private val activity: Activity) {
    private val spreadsheetId = "id of spreadsheet (from url)"
    private var pendingData: List<String>? = null

    val requestedScopes = listOf(
        Scope("https://www.googleapis.com/auth/spreadsheets")
    )

    val authorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(requestedScopes)
        .build()

    fun startAuthorization(launcher: ActivityResultLauncher<IntentSenderRequest>, data: List<String>) {
        Log.d("MainaActivity", "startAuthorization run with data: $data")
        this.pendingData = data
        Identity.getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    launcher.launch(IntentSenderRequest.Builder(pendingIntent!!.intentSender).build())
                } else {
                    updateSheet(authorizationResult)
                }
            }
            .addOnFailureListener { e -> Log.e("apiCaller", "Failed to authorize", e) }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun saveToOfflineCache(data: List<String>) {
        try {
            val file = File(activity.filesDir, "pending_updates.csv")
            val csvLine = data.joinToString(",") + "\n"
            FileOutputStream(file, true).use { it.write(csvLine.toByteArray()) }
            Log.d("ApiCaller", "Saved to offline cache: $csvLine")
        } catch (e: Exception) {
            Log.e("ApiCaller", "Failed to save offline cache", e)
        }
    }

    private fun scheduleSync(accessToken: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)        .build()

        val syncData = workDataOf(
            "spreadsheetId" to spreadsheetId,
            "accessToken" to accessToken
        )

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(syncData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        // Use REPLACE because SyncWorker processes the entire file anyway
        WorkManager.getInstance(activity.applicationContext).enqueueUniqueWork(
            "spreadsheetSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        Log.d("ApiCaller", "Sync scheduled via WorkManager")
    }

    fun updateSheet(authorizationResult: AuthorizationResult) {
        Log.d("MainaActivity", "updatesheet run")
        val accessToken = authorizationResult.accessToken ?: return
        val dataToSave = pendingData ?: listOf("Default", System.currentTimeMillis().toString())

        if (!isOnline()) {
            Log.d("ApiCaller", "Device offline, saving locally")
            saveToOfflineCache(dataToSave)
            scheduleSync(accessToken)
            return
        }

        Thread {
            Log.d("MainaActivity", "Thread run")
            try {
                val transport = com.google.api.client.http.javanet.NetHttpTransport()
                val jsonFactory = com.google.api.client.json.gson.GsonFactory.getDefaultInstance()
                val service = Sheets.Builder(transport, jsonFactory, null)
                    .setApplicationName("Spreadsheet Updater")
                    .setHttpRequestInitializer { request ->
                        request.headers.authorization = "Bearer $accessToken"
                    }
                    .build()

                val appendBody = ValueRange().setValues(listOf(dataToSave))
                service.spreadsheets().values()
                    .append(spreadsheetId, "tracker!A1", appendBody)
                    .setValueInputOption("RAW")
                    .execute()
                Log.d("ApiCaller", "Successfully updated online")
            } catch (e: Exception) {
                Log.e("ApiCaller", "Online update failed, caching offline", e)
                saveToOfflineCache(dataToSave)
                scheduleSync(accessToken)
            }
        }.start()
    }
}
