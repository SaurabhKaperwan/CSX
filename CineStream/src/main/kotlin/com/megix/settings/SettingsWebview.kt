package com.megix.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import com.megix.settings.SettingsTheme.dp


//WebView login dialog for extracting authentication tokens.
@SuppressLint("SetJavaScriptEnabled")
internal object SettingsWebView {

    private const val TAG = "CineStreamAuth"

    enum class LoginType { SHOWBOX, GRAMCINEMA }

    private data class Config(
        val title: String,
        val startUrl: String,
        val accentColor: Int,
        val accentColorB: Int,
        val tokenLabel: String,
        val tokenCookieName: String,
        val extractJs: String
    )

    private fun configFor(type: LoginType) = when (type) {
        LoginType.SHOWBOX -> Config(
            "Login to Febbox", "https://www.febbox.com/",
            Color.parseColor("#F59E0B"), Color.parseColor("#D97706"),
            "UI Token", "ui",
            "(()=>{let m=document.cookie.match(/(?:^|;\\s*)ui=([^;]*)/);return m?decodeURIComponent(m[1]):''})()"
        )
        LoginType.GRAMCINEMA -> Config(
            "Login to GramCinema", "https://bollywood.eu.org/",
            Color.parseColor("#F472B6"), Color.parseColor("#EC4899"),
            "Bearer Token", "bearer_token",
            "(()=>{const isReal=t=>t&&t.startsWith('eyJ0eXAiOi')&&!t.includes(' ');for(let k of ['token','bearer_token','access_token','auth_token','jwt','bearerToken','authToken','user_token','userToken','accessToken','authAccessToken']){let t=localStorage.getItem(k)||sessionStorage.getItem(k);if(isReal(t)){console.log('Found real token via key: '+k);return t;}}for(let i=0;i<localStorage.length;i++){let v=(localStorage.getItem(localStorage.key(i))||'').replace(/^Bearer\\s+/i,'').replace(/[\"']/g,'').trim();if(isReal(v)){console.log('Found real token hidden in storage!');return v;}}return '';})()"
        )
    }

    // CF_BYPASS_USER_AGENT for cookie validation
    private const val CF_BYPASS_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"
    private const val SPOOF_JS = "if(!window.__csxSpoofed){window.__csxSpoofed=!0;let ua='Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36';try{Object.defineProperty(navigator,'userAgent',{get:()=>ua});Object.defineProperty(navigator,'appVersion',{get:()=>ua.replace('Mozilla/','')})}catch(e){}if(!window.chrome)window.chrome={runtime:{},loadTimes:()=>{},csi:()=>{}}}"

    private const val GRAMCINEMA_INTERCEPT_JS = "if(!window.__csxTokenHooked){window.__csxTokenHooked=!0;const isReal=s=>s&&s.startsWith('eyJ0eXAiOi')&&!s.includes(' ');let _set=XMLHttpRequest.prototype.setRequestHeader;XMLHttpRequest.prototype.setRequestHeader=function(n,v){if(n.toLowerCase()==='authorization'){let t=v.replace(/^Bearer\\s+/i,'').trim();if(isReal(t)){console.log('Intercepted real XHR token!');try{Android.onToken(t)}catch(e){}}}return _set.apply(this,arguments)};let _f=window.fetch;if(_f)window.fetch=function(u,i){try{if(i&&i.headers){let h=i.headers,a=typeof h.get==='function'?h.get('Authorization'):(h.Authorization||h.authorization||'');if(a){let t=a.replace(/^Bearer\\s+/i,'').trim();if(isReal(t)){console.log('Intercepted real Fetch token!');Android.onToken(t)}}}}catch(e){}return _f.apply(this,arguments)}}"

    private class TokenBridge(private val deliver: (String) -> Unit) {
        @JavascriptInterface
        fun onToken(value: String) {
            Log.d(TAG, "Native Bridge received token from JS! Length: ${value.length}")
            deliver(value)
        }
    }

    fun showCloudflareBypass(context: Context, startUrl: String, onCookiesChanged: () -> Unit = {}) {
        val theme = SettingsTheme
        val mainHandler = Handler(Looper.getMainLooper())
        val targetUrl = startUrl.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }

        var currentUrl = targetUrl
        var dialogRef: Dialog? = null

        val statusText = TextView(context).apply {
            id = View.generateViewId()
            text = targetUrl; textSize = 10f; setTextColor(theme.TEXT_SECONDARY); setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateStatus(url: String) {
            currentUrl = url
            mainHandler.post { statusText.text = url }
        }

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3.dp(context))
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#22C55E"))
            max = 100; progress = 10
        }

        val webViewContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val cookieText = TextView(context).apply {
            text = "Cookies: <none>"
            textSize = 11f; setTextColor(theme.TEXT_SECONDARY)
            setPadding(16.dp(context), 6.dp(context), 16.dp(context), 6.dp(context))
        }

        fun refreshCookieDisplay(url: String) {
            val cookies = CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() } ?: "<none>"
            mainHandler.post {
                cookieText.text = "Cookies: $cookies"
            }
            Log.d(TAG, "Cloudflare bypass cookies for $url: $cookies")
        }

        val cookieControls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp(context), 6.dp(context), 16.dp(context), 6.dp(context))
            addView(SettingsWidgets.pillBtn(context, "💾 Save Cookies", Color.parseColor("#10B981"), Color.parseColor("#0B1120"), Color.parseColor("#059669")) {
                val cookies = CookieManager.getInstance().getCookie(currentUrl)
                if (!cookies.isNullOrBlank()) {
                    Settings.saveCookieForDomain(currentUrl, cookies)
                    Log.d(TAG, "Saved cookies for $currentUrl: ${cookies.take(50)}...")
                    refreshCookieDisplay(currentUrl)
                    onCookiesChanged()
                    Toast.makeText(context, "Cookies saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No cookies to save", Toast.LENGTH_SHORT).show()
                }
            })
        }

        val webView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.WHITE)
            isFocusable = true; isFocusableInTouchMode = true

            settings.apply {
                javaScriptEnabled = true; domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                useWideViewPort = true; loadWithOverviewMode = true
                setSupportZoom(false); builtInZoomControls = false
                userAgentString = CF_BYPASS_USER_AGENT
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.progress = newProgress
                    progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    updateStatus(url)
                    // Load saved cookies for this domain
                    val savedCookie = Settings.getCookieForDomain(url)
                    if (!savedCookie.isNullOrBlank()) {
                        CookieManager.getInstance().setCookie(url, savedCookie)
                        Log.d(TAG, "Loaded saved cookies for $url: ${savedCookie.take(50)}...")
                        Toast.makeText(context, "Loaded saved cookies", Toast.LENGTH_SHORT).show()
                    }
                    Log.d(TAG, "Cloudflare bypass WebView started: $url")
                }

                override fun onPageFinished(view: WebView, url: String) {
                    updateStatus(url)
                    view.evaluateJavascript(SPOOF_JS, null)
                    refreshCookieDisplay(url)
                    Log.d(TAG, "Cloudflare bypass WebView finished: $url")
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
            }
        }

        webViewContainer.addView(webView)

        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp(context), 12.dp(context), 10.dp(context), 12.dp(context))
            background = theme.colorDrawable(theme.BG_CARD)

            addView(SettingsWidgets.accentBar(context, Color.parseColor("#22C55E"), Color.parseColor("#16A34A")))
            addView(TextView(context).apply {
                text = "🌐 Cloudflare Bypass"; textSize = 14f; setTypeface(null, Typeface.BOLD)
                setTextColor(theme.TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(SettingsWidgets.hSpacer(context, 8))
            addView(SettingsWidgets.pillBtn(context, "✕ Close", theme.TEXT_SECONDARY, theme.BG_CARD, Color.parseColor("#2A2D3E")) { dialogRef?.dismiss() })
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; background = theme.colorDrawable(theme.BG_DARK)
            addView(titleBar); addView(progressBar); addView(webViewContainer)
            addView(cookieControls)
            addView(cookieText)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(16.dp(context), 6.dp(context), 16.dp(context), 8.dp(context))
                background = theme.colorDrawable(theme.BG_CARD)
                addView(statusText)
            })
        }

        dialogRef = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(root); setCancelable(true)
            setOnDismissListener {
                webView.stopLoading(); webView.destroy()
                onCookiesChanged()
            }
            window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                setBackgroundDrawable(theme.colorDrawable(theme.BG_DARK))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    @Suppress("DEPRECATION") setDecorFitsSystemWindows(false)
                    root.setOnApplyWindowInsetsListener { view, insets ->
                        val ime = insets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemBars())
                        view.setPadding(ime.left, ime.top, ime.right, ime.bottom)
                        insets
                    }
                } else {
                    @Suppress("DEPRECATION") setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            }
            show()
        }

        webView.clearCache(true)
        webView.clearHistory()
        webView.loadUrl(targetUrl)
        webView.requestFocus()
    }

    fun show(context: Context, type: LoginType, onToken: (String) -> Unit) {
        val cfg = configFor(type)
        val theme = SettingsTheme
        val mainHandler = Handler(Looper.getMainLooper())

        var tokenCaptured = false
        var currentUrl = cfg.startUrl
        var dialogRef: Dialog? = null

        fun deliver(raw: String) {
            if (tokenCaptured) return
            val token = raw.trim().removeSurrounding("\"").trim()
            if (token.isBlank() || token == "null") return

            tokenCaptured = true
            Log.i(TAG, "SUCCESS! Token captured for ${type.name}.")

            mainHandler.post {
                onToken(token)
                Toast.makeText(context, "✓ ${cfg.tokenLabel} captured — tap outer Save to apply", Toast.LENGTH_SHORT).show()
                dialogRef?.dismiss()
            }
        }

        fun clearWebLoginSession(onCleared: () -> Unit) {
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies { _ ->
                CookieManager.getInstance().flush()
                mainHandler.post { onCleared() }
            }
        }

        val statusText = TextView(context).apply {
            text = cfg.startUrl; textSize = 10f; setTextColor(theme.TEXT_SECONDARY); setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3.dp(context))
            progressTintList = android.content.res.ColorStateList.valueOf(cfg.accentColor)
            max = 100; progress = 10
        }

        // FRAME LAYOUT CONTAINER: Allows us to stack popup WebViews on top of the main one
        val webViewContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val webView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.WHITE)
            isFocusable = true; isFocusableInTouchMode = true

            settings.apply {
                javaScriptEnabled = true; domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                useWideViewPort = true; loadWithOverviewMode = true
                setSupportZoom(false); builtInZoomControls = false
                userAgentString = CF_BYPASS_USER_AGENT

                // Enable Multi-Window Support for Google Sign-in popups
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            addJavascriptInterface(TokenBridge(::deliver), "Android")

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.progress = newProgress
                    progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                }
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(TAG, "JS: ${consoleMessage?.message()}")
                    return super.onConsoleMessage(consoleMessage)
                }

                // Handles the popup creation when user clicks "Log in with Google"
                override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    Log.d(TAG, "Creating new popup window for OAuth flow.")
                    val popupWebView = WebView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = view.settings.userAgentString
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onCloseWindow(window: WebView) {
                                Log.d(TAG, "Popup window called close(). Destroying overlay.")
                                webViewContainer.removeView(window)
                                window.destroy()
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
                        }
                    }
                    webViewContainer.addView(popupWebView)
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = popupWebView
                    resultMsg?.sendToTarget()
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                private fun updateUrl(url: String) { currentUrl = url; mainHandler.post { statusText.text = url } }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) = updateUrl(url)

                override fun onPageFinished(view: WebView, url: String) {
                    updateUrl(url)
                    view.evaluateJavascript(SPOOF_JS, null)

                    if (type == LoginType.GRAMCINEMA) {
                        view.evaluateJavascript(GRAMCINEMA_INTERCEPT_JS, null)
                        var pollCount = 0
                        val poll = object : Runnable {
                            override fun run() {
                                if (tokenCaptured || pollCount++ >= 40) return
                                view.evaluateJavascript(cfg.extractJs) { res ->
                                    val clean = res?.trim()?.removeSurrounding("\"")?.trim()
                                    if (!clean.isNullOrBlank() && clean != "null") deliver(clean)
                                    else mainHandler.postDelayed(this, 1500)
                                }
                            }
                        }
                        mainHandler.postDelayed(poll, 1500)
                    }

                    val cookieToken = CookieManager.getInstance().getCookie(url)?.split(";")
                        ?.map { it.trim() }?.firstOrNull { it.startsWith("${cfg.tokenCookieName}=") }
                        ?.removePrefix("${cfg.tokenCookieName}=")

                    if (!cookieToken.isNullOrBlank()) { deliver(cookieToken); return }

                    if (!tokenCaptured && cfg.extractJs.isNotBlank()) {
                        view.evaluateJavascript(cfg.extractJs) { res ->
                            val clean = res?.trim()?.removeSurrounding("\"")?.trim()
                            if (!clean.isNullOrBlank() && clean != "null") deliver(clean)
                        }
                    }
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
            }
        }

        webViewContainer.addView(webView)

        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp(context), 12.dp(context), 10.dp(context), 12.dp(context))
            background = theme.colorDrawable(theme.BG_CARD)

            addView(SettingsWidgets.accentBar(context, cfg.accentColor, cfg.accentColorB))
            addView(TextView(context).apply {
                text = cfg.title; textSize = 14f; setTypeface(null, Typeface.BOLD)
                setTextColor(theme.TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(SettingsWidgets.hSpacer(context, 8))
            addView(SettingsWidgets.pillBtn(context, "✕ Close", theme.TEXT_SECONDARY, theme.BG_CARD, Color.parseColor("#2A2D3E")) { dialogRef?.dismiss() })
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; background = theme.colorDrawable(theme.BG_DARK)
            addView(titleBar); addView(progressBar); addView(webViewContainer)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(16.dp(context), 6.dp(context), 16.dp(context), 8.dp(context))
                background = theme.colorDrawable(theme.BG_CARD)
                addView(statusText)
            })
        }

        dialogRef = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(root); setCancelable(true)
            setOnDismissListener { webView.stopLoading(); webView.destroy() }
            window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                setBackgroundDrawable(theme.colorDrawable(theme.BG_DARK))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    @Suppress("DEPRECATION") setDecorFitsSystemWindows(false)
                    root.setOnApplyWindowInsetsListener { view, insets ->
                        val ime = insets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemBars())
                        view.setPadding(ime.left, ime.top, ime.right, ime.bottom)
                        insets
                    }
                } else {
                    @Suppress("DEPRECATION") setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            }
            show()
        }

        clearWebLoginSession {
            webView.clearCache(true)
            webView.clearHistory()
            webView.loadUrl(cfg.startUrl)
            webView.requestFocus()
        }
    }
}
