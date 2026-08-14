package com.nexflix.app

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineLayout: android.widget.LinearLayout

    private val baseUrl by lazy { getString(R.string.base_url) }
    private val baseHost by lazy { Uri.parse(baseUrl).host ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        offlineLayout = findViewById(R.id.offlineLayout)
        val retryButton = findViewById<android.widget.Button>(R.id.retryButton)

        setupWebView()
        setupSwipeRefresh()
        setupDownloads()

        retryButton.setOnClickListener { loadStart() }

        // Ask for notification permission (Android 13+)
        requestNotificationPermissionIfNeeded()

        // Subscribe to the "new posts" topic so the RSS -> FCM script can notify everyone
        FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
        // Subscribe to "app_updates" so users get a push notification whenever you release an update
        FirebaseMessaging.getInstance().subscribeToTopic("app_updates")

        // Force-update check runs every launch
        checkForForceUpdate()

        // If app was opened by tapping a post notification, load that post directly
        val postUrl = intent.getStringExtra("post_url")
        if (savedInstanceState == null) {
            if (!postUrl.isNullOrEmpty()) {
                loadUrl(postUrl)
            } else {
                loadStart()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val postUrl = intent.getStringExtra("post_url")
        if (!postUrl.isNullOrEmpty()) {
            loadUrl(postUrl)
        }
    }

    private fun loadStart() {
        if (isOnline()) {
            offlineLayout.visibility = android.view.View.GONE
            webView.visibility = android.view.View.VISIBLE
            webView.loadUrl(baseUrl)
        } else {
            offlineLayout.visibility = android.view.View.VISIBLE
            webView.visibility = android.view.View.GONE
        }
    }

    private fun loadUrl(url: String) {
        if (isOnline()) {
            offlineLayout.visibility = android.view.View.GONE
            webView.visibility = android.view.View.VISIBLE
            webView.loadUrl(url)
        } else {
            offlineLayout.visibility = android.view.View.VISIBLE
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Suppress("DEPRECATION")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false

        // No URL bar anywhere: everything - including other pages/domains you link to - opens inside this WebView
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                val scheme = request.url.scheme ?: ""

                // Let phone-native actions (call, email, whatsapp, other apps) open outside the app
                if (scheme != "http" && scheme != "https") {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        true
                    } catch (e: Exception) {
                        true // no app installed to handle it; just ignore
                    }
                }

                // Everything else (same site or any other http/https link) stays inside the app
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = android.view.View.GONE
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    offlineLayout.visibility = android.view.View.VISIBLE
                    webView.visibility = android.view.View.GONE
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.brand)
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    // Files download inside a "Nexflix" folder under the device's Downloads directory
    private fun setupDownloads() {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file")
                request.setTitle(fileName)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.allowScanningByMediaScanner()

                // Saves to: Downloads/Nexflix/filename  (app-named subfolder)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "Nexflix/$fileName"
                )

                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Downloading to Downloads/Nexflix", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    // Reads version.json from GitHub and forces an update if a newer version is required
    private fun checkForForceUpdate() {
        Thread {
            try {
                val url = getString(R.string.version_check_url)
                val text = URL(url).readText()
                val json = JSONObject(text)
                val latestVersionCode = json.getInt("latestVersionCode")
                val forceUpdate = json.optBoolean("forceUpdate", false)
                val apkUrl = json.getString("apkUrl")
                val currentVersionCode = BuildConfig.VERSION_CODE

                if (latestVersionCode > currentVersionCode) {
                    runOnUiThread {
                        showUpdateDialog(apkUrl, forceUpdate)
                    }
                }
            } catch (e: Exception) {
                Log.e("Nexflix", "Version check failed", e)
            }
        }.start()
    }

    private fun showUpdateDialog(apkUrl: String, forceUpdate: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update available")
        builder.setMessage("A new version of Nexflix is available. Please update to continue.")
        builder.setCancelable(!forceUpdate)
        builder.setPositiveButton("Update Now") { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
            if (forceUpdate) finish()
        }
        if (!forceUpdate) {
            builder.setNegativeButton("Later", null)
        }
        builder.show()
    }

    // Back button navigates WebView history first, like a real app - doesn't just close
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
