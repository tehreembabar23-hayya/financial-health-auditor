package com.tehreembabar23hayya.financialhealthauditor.detection

import android.content.Context
import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ResolutionService {

    suspend fun generateDraft(
        context: Context,
        transaction: Transaction,
        explanation: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext "Dear ${transaction.merchant},\n\nI would like to cancel my subscription of PKR ${transaction.amount} immediately.\n\nThank you."
        }

        val prompt = "Write a short, polite email requesting cancellation of this subscription: merchant=${transaction.merchant}, amount=${transaction.amount}. Keep it under 80 words."

        try {
            val urlConnection = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.doOutput = true

            val requestBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            OutputStreamWriter(urlConnection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            if (urlConnection.responseCode == 200) {
                val responseText = urlConnection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text").trim()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        "Dear ${transaction.merchant},\n\nI would like to cancel my subscription of PKR ${transaction.amount} immediately.\n\nThank you."
    }

    private fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("secrets_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("gemini_api_key", null)
        if (!key.isNullOrBlank()) return key

        try {
            val resId = context.resources.getIdentifier("gemini_api_key", "string", context.packageName)
            if (resId != 0) {
                return context.getString(resId)
            }
        } catch (e: Exception) {}

        return ""
    }
}
