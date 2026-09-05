package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.example.data.audio.PodcastAudioWriter
import com.example.data.pdf.PdfHelper
import com.example.BuildConfig
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

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext PodcastGenerationResult(
                isSuccess = false,
                errorMessage = "Clé d'API Gemini non configurée. Veuillez ajouter votre GEMINI_API_KEY dans le panneau des Secrets de Google AI Studio."
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

            // Cap length to ~12,000 chars for optimal Gemini processing
            val truncatedText = fullText.take(12000)

            onProgress("Écriture du script pédagogique avec Gemini IA...")
            val scriptLines = generatePodcastScript(apiKey, pdfTitle, truncatedText)

            if (scriptLines.isEmpty()) {
                return@withContext PodcastGenerationResult(
                    isSuccess = false,
                    errorMessage = "Impossible de générer le script du podcast. Réessayez plus tard."
                )
            }

            val scriptJson = JSONArray().apply {
                scriptLines.forEach { line ->
                    put(JSONObject().apply {
                        put("speaker", line.speaker)
                        put("text", line.text)
                    })
                }
            }.toString()

            onProgress("Génération de la synthèse vocale HD (Professeur & Élève)...")
            val audioChunks = mutableListOf<ByteArray>()

            scriptLines.forEachIndexed { index, line ->
                onProgress("Synthèse vocale : Réplique ${index + 1} sur ${scriptLines.size}...")
                val voiceName = if (line.speaker == "PROFESSEUR") "Puck" else "Fenrir"
                val chunk = generateTtsAudioChunk(apiKey, line.text, voiceName)
                if (chunk != null && chunk.isNotEmpty()) {
                    audioChunks.add(chunk)
                }
            }

            if (audioChunks.isEmpty()) {
                return@withContext PodcastGenerationResult(
                    isSuccess = false,
                    errorMessage = "La synthèse vocale n'a pas pu produire le fichier audio."
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

    private fun generatePodcastScript(apiKey: String, pdfTitle: String, pdfText: String): List<ScriptLine> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val promptText = """
            Tu es un concepteur pédagogique expert et un podcasteur chevronné.
            À partir du cours PDF intitulé "$pdfTitle", rédige un court épisode de podcast pédagogique sous forme de dialogue entre deux personnes :
            1. PROFESSEUR (expert, bienveillant, structuré, qui explique clairement les concepts clés).
            2. ELEVE (curieux, motivé, pose des questions pertinentes et résume en ses propres mots).

            Consignes impératives :
            - Langue : Français naturel, fluide et captivant.
            - Format : Réponds STRICTEMENT avec un tableau JSON d'objets sans aucun texte additionnel ni balise markdown autour, sous la forme :
            [
              {"speaker": "PROFESSEUR", "text": "Bonjour et bienvenue dans notre épisode révision sur $pdfTitle !"},
              {"speaker": "ELEVE", "text": "Bonjour professeur ! Quels sont les points essentiels à retenir aujourd'hui ?"},
              ...
            ]
            - Génère entre 6 et 10 répliques équilibrées et claires.

            Voici le contenu du PDF :
            $pdfText
        """.trimIndent()

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", promptText)
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        return parseScriptResponseJson(responseBody)
    }

    private fun parseScriptResponseJson(responseBody: String): List<ScriptLine> {
        val resultList = mutableListOf<ScriptLine>()
        try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            val candidate = candidates.optJSONObject(0) ?: return emptyList()
            val content = candidate.optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            var rawText = parts.optJSONObject(0)?.optString("text") ?: return emptyList()

            // Clean markdown codeblocks if present
            rawText = rawText.trim()
            if (rawText.startsWith("```json")) {
                rawText = rawText.removePrefix("```json")
            }
            if (rawText.startsWith("```")) {
                rawText = rawText.removePrefix("```")
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.removeSuffix("```")
            }
            rawText = rawText.trim()

            val jsonArray = JSONArray(rawText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val speaker = obj.optString("speaker", "PROFESSEUR").uppercase()
                val text = obj.optString("text", "")
                if (text.isNotBlank()) {
                    resultList.add(ScriptLine(speaker = speaker, text = text))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiPodcastService", "Failed to parse script JSON response", e)
        }
        return resultList
    }

    private fun generateTtsAudioChunk(apiKey: String, text: String, voiceName: String): ByteArray? {
        // Primary Gemini TTS Endpoint
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", text)
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiPodcastService", "Gemini TTS endpoint returned ${response.code}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            val candidate = candidates.optJSONObject(0) ?: return null
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val firstPart = parts.optJSONObject(0) ?: return null
            val inlineData = firstPart.optJSONObject("inlineData") ?: return null
            val base64Data = inlineData.optString("data", "")

            if (base64Data.isNotBlank()) {
                return Base64.decode(base64Data, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            Log.e("GeminiPodcastService", "TTS Chunk generation failed", e)
        }
        return null
    }
}
