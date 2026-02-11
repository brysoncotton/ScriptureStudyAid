package com.example.scripturestudyaid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import androidx.appcompat.app.AlertDialog

class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnReadScriptures = findViewById<Button>(R.id.btnReadScriptures)
        val btnCompare = findViewById<Button>(R.id.btnCompare)
        val btnAdvancedSearch = findViewById<Button>(R.id.btnAdvancedSearch)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnReadScriptures.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnCompare.setOnClickListener {
            val intent = Intent(this, ComparisonSelectionActivity::class.java)
            startActivity(intent)
        }

        btnAdvancedSearch.setOnClickListener {
            val intent = Intent(this, AdvancedSearchActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        checkForUpdates()
    }

    private fun checkForUpdates() {
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }

        GithubUpdateChecker.checkForUpdate(currentVersion) { latestVersion, updateUrl ->
            showUpdateDialog(latestVersion, updateUrl)
        }
    }

    private fun showUpdateDialog(latestVersion: String, updateUrl: String) {
        if (isFinishing || isDestroyed) return
        
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version of Scripture Study Aid is available ($latestVersion).\n\nDo you want to proceed to Github to download the latest version?")
            .setPositiveButton("Update") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
