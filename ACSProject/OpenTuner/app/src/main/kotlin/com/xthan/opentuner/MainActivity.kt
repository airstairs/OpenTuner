package com.xthan.opentuner

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val permissionRequestCode = 101
    private lateinit var webView: WebView
    private var pendingPermissionRequest: PermissionRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Configure WebView Settings
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // Handle Web Chrome permissions (Microphone/Audio capture)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    for (resource in request.resources) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE == resource) {
                            if (checkAudioPermission()) {
                                request.grant(request.resources)
                            } else {
                                pendingPermissionRequest = request
                                requestAudioPermission()
                            }
                            return@runOnUiThread
                        }
                    }
                    request.grant(request.resources)
                }
            }
        }

        webView.webViewClient = WebViewClient()

        // Check Native Android Permissions on Launch
        if (!checkAudioPermission()) {
            requestAudioPermission()
        }

        // Load local asset HTML file
        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html")
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            ),
            permissionRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionRequest?.let { request ->
                    request.grant(request.resources)
                    pendingPermissionRequest = null
                }
            } else {
                pendingPermissionRequest?.let { request ->
                    request.deny()
                    pendingPermissionRequest = null
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Prevents activity reload on rotation while allowing responsive CSS to handle layout changes
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
