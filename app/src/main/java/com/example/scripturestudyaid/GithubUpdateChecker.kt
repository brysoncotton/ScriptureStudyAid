package com.example.scripturestudyaid

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object GithubUpdateChecker {

    private const val REPO_OWNER = "brysoncotton"
    private const val REPO_NAME = "ScriptureStudyAid"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    fun checkForUpdate(currentVersion: String, onUpdateAvailable: (String, String) -> Unit) {
        thread {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val json = JSONObject(response.toString())
                    val tagName = json.getString("tag_name") // e.g., "v1.1" or "1.1"
                    val htmlUrl = json.getString("html_url")

                    // Clean the version string (remove 'v' or 'v.' or other non-numeric prefixes)
                    val versionRegex = "[^0-9]*([0-9]+.*)".toRegex()
                    
                    val cleanTagName = versionRegex.find(tagName)?.groupValues?.get(1) ?: tagName
                    val cleanCurrentVersion = versionRegex.find(currentVersion)?.groupValues?.get(1) ?: currentVersion

                    android.util.Log.d("GithubUpdateChecker", "Checking update: Remote='$tagName' ($cleanTagName) vs Local='$currentVersion' ($cleanCurrentVersion)")

                    if (isNewerVersion(cleanTagName, cleanCurrentVersion)) {
                        Handler(Looper.getMainLooper()).post {
                            onUpdateAvailable(tagName, htmlUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail silently on network errors or parsing issues
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until length) {
            val l = if (i < latestParts.size) latestParts[i] else 0
            val c = if (i < currentParts.size) currentParts[i] else 0

            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
