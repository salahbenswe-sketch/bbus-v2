package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.BusTrackingData
import com.example.util.LocalAssetServer

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BusMapView(
    bus1Data: BusTrackingData?,
    bus2Data: BusTrackingData? = null,
    bus3Data: BusTrackingData? = null,
    selectedBusIndex: Int = 1,
    reCenterTimestamp: Long = 0L,
    reCenterBusIndex: Int = 1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val port = remember { LocalAssetServer.start(context) }
    val mapUrl = "http://127.0.0.1:$port/leaflet_map.html"

    val webView = remember(mapUrl) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                allowContentAccess = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("BusMapViewJS", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    bus1Data?.let { d ->
                        val safeTime = d.formattedFixTime.replace("'", "\\'")
                        val js = "if (typeof updateBus === 'function') { updateBus(1, '${d.busName}', ${d.latitude}, ${d.longitude}, ${d.speedKmh}, ${d.courseDegrees}, '$safeTime', ${d.isOnline}); }"
                        view?.evaluateJavascript(js, null)
                    }
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    return true
                }
            }
            loadUrl(mapUrl)
        }
    }

    LaunchedEffect(bus1Data) {
        bus1Data?.let { d ->
            val safeTime = d.formattedFixTime.replace("'", "\\'")
            val js = "if (typeof updateBus === 'function') { updateBus(1, '${d.busName}', ${d.latitude}, ${d.longitude}, ${d.speedKmh}, ${d.courseDegrees}, '$safeTime', ${d.isOnline}); }"
            webView.evaluateJavascript(js, null)
        }
    }

    LaunchedEffect(reCenterTimestamp) {
        if (reCenterTimestamp > 0) {
            val js = "if (typeof centerBus === 'function') { centerBus(1); }"
            webView.evaluateJavascript(js, null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        AndroidView(
            factory = { webView },
            update = { view ->
                if (view.url == null) {
                    view.loadUrl(mapUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BusMapView(
    busData: BusTrackingData?,
    reCenterTimestamp: Long,
    modifier: Modifier = Modifier
) {
    BusMapView(
        bus1Data = busData,
        bus2Data = null,
        bus3Data = null,
        selectedBusIndex = 1,
        reCenterTimestamp = reCenterTimestamp,
        reCenterBusIndex = 1,
        modifier = modifier
    )
}
