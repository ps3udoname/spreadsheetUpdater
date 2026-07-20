package com.example.spreadsheetupdater

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.File

class SyncWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("SyncWorker", "doWork() started") // Use a clear log

        val spreadsheetId = inputData.getString("spreadsheetId")
        val accessToken = inputData.getString("accessToken")

        if (spreadsheetId == null || accessToken == null) {
            Log.e("SyncWorker", "Missing input data: id=$spreadsheetId, token=${accessToken?.take(5)}...")
            return Result.failure()
        }

        val csvFile = File(applicationContext.filesDir, "pending_updates.csv")
        if (!csvFile.exists()) {
            Log.d("SyncWorker", "No cache file found, nothing to sync.")
            return Result.success()
        }

        val transport = com.google.api.client.http.javanet.NetHttpTransport()
        val jsonFactory = com.google.api.client.json.gson.GsonFactory.getDefaultInstance()
        val service = Sheets.Builder(transport, jsonFactory, null)
            .setApplicationName("Spreadsheet Updater")
            .setHttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $accessToken"
            }
            .build()

        val lines = try {
            csvFile.readLines()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to read CSV file", e)
            return Result.failure()
        }

        val failedLines = mutableListOf<String>()

        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.split(",")

            // Now we use all parts of the CSV line (amount, category, note, date)
            val valuesToAppend = listOf(parts)
            val appendBody = ValueRange().setValues(valuesToAppend)

            try {
                service.spreadsheets().values()
                    .append(spreadsheetId, "tracker!A1", appendBody)
                    .setValueInputOption("RAW")
                    .execute()
                Log.d("SyncWorker", "Successfully synced line: $line")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Error syncing line (will retry): $line", e)
                failedLines.add(line)
            }
        }

        return if (failedLines.isEmpty()) {
            csvFile.delete()
            Log.d("SyncWorker", "Sync complete. Cache cleared.")
            Result.success()
        } else {
            csvFile.writeText(failedLines.joinToString("\n"))
            Log.w("SyncWorker", "Sync failed for ${failedLines.size} lines. Scheduling retry.")
            Result.retry()
        }
    }
}
