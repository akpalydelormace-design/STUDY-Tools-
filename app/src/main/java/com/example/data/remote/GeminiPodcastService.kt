package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.example.data.audio.PodcastAudioWriter
import com.example.data.pdf.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class ScriptLine(
    val speaker: String, // "PROFESSEUR" or "ELEVE"
    val text: String
)

data class PodcastGenerationResult(
    val isSuccess: Boolean,
    val audioFile: File? = null,
    val durationMs: Long = 0L,
    val scriptJson: String = "",
    val errorMessage: String? = null
)

class GeminiPodcastService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Backend endpoint options (10.0.2.2 for Android Emulator, localhost for local JVM tests)
    private val backendEndpoints = listOf(
        "http://10.0.2.2:3000/api/podcast/generate",
        "http://localhost:3000/api/podcast/generate"
    )

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }

    suspend fun generatePodcastFromPdf(
        pdfFile: File,
        pdfTitle: String,
        pdfId: Long,
        onProgress: (statusMessage: String) -> Unit
    ): PodcastGenerationResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext PodcastGenerationResult(
                isSuccess = false,
                errorMessage = "La génération du podcast nécessite une connexion Internet active."
            )
        }

        try {
            onProgress("Extraction du texte du cours PDF...")
            val extractedMap = PdfHelper.extractTextByPages(pdfFile)
            val fullText = extractedMap.values.joinToString("\n\n")

            if (fullText.trim().length < 50) {
                return@withContext PodcastGenerationResult(
                    isSuccess = false,
                    errorMessage = "Le document PDF ne contient pas suffisamment de texte pour générer un podcast."
                )
            }

            // Cap length to ~12,000 chars for optimal backend processing
            val truncatedText = fullText.take(12000)

            onProgress("Génération sécurisée via le serveur backend IA...")
            
            val payload = JSONObject().apply {
                put("pdfTitle", pdfTitle)
                put("pdfText", truncatedText)
            }

            var backendResponseJson: JSONObject? = null
            var lastErrorMessage: String? = null

            for (endpoint in backendEndpoints) {
                try {
                    val request = Request.Builder()
                        .url(endpoint)
                        .post(payload.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful && bodyStr.isNotBlank()) {
                        backendResponseJson = JSONObject(bodyStr)
                        break
                    } else {
                        if (bodyStr.isNotBlank()) {
                            val errObj = JSONObject(bodyStr)
                            lastErrorMessage = errObj.optString("errorMessage", "Erreur serveur HTTP ${response.code}")
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMessage = e.localizedMessage
                }
            }

            if (backendResponseJson == null || !backendResponseJson.optBoolean("isSuccess", false)) {
                val err = backendResponseJson?.optString("errorMessage", lastErrorMessage ?: "Impossible de joindre le serveur backend sécurisé.")
                return@withContext PodcastGenerationResult(
                    isSuccess = false,
                    errorMessage = err
                )
            }

            val scriptJson = backendResponseJson.optString("scriptJson", "[]")
            val audioChunksArray = backendResponseJson.optJSONArray("audioChunksBase64")
            val audioChunks = mutableListOf<ByteArray>()

            if (audioChunksArray != null) {
                for (i in 0 until audioChunksArray.length()) {
                    val b64Str = audioChunksArray.getString(i)
                    if (b64Str.isNotBlank()) {
                        audioChunks.add(Base64.decode(b64Str, Base64.DEFAULT))
                    }
                }
            }

            if (audioChunks.isEmpty()) {
                return@withContext PodcastGenerationResult(
                    isSuccess = false,
                    errorMessage = "Le serveur backend n'a pas pu produire les éléments audio."
                )
            }

            onProgress("Finalisation de l'enregistrement audio local...")
            val (audioFile, durationMs) = PodcastAudioWriter.saveBase64AudioChunksToFile(
                context = context,
                pdfId = pdfId,
                audioBase64Chunks = audioChunks
            )

            PodcastGenerationResult(
                isSuccess = true,
                audioFile = audioFile,
                durationMs = durationMs,
                scriptJson = scriptJson,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e("GeminiPodcastService", "Error generating podcast", e)
            PodcastGenerationResult(
                isSuccess = false,
                errorMessage = "Erreur de génération : ${e.localizedMessage ?: "Erreur inconnue"}"
            )
        }
    }
}
