package com.github.eprendre.tingshu.sources

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.extensions.extractorAsyncExecute
import com.github.eprendre.tingshu.extensions.getDesktopUA
import com.github.eprendre.tingshu.extensions.getMobileUA
import com.github.eprendre.tingshu.widget.PlaybackBridge
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val DEFAULT_HTML_SCRIPT =
    "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"

/**
 * 章节地址本身就是音频地址
 */
object AudioUrlDirectExtractor : AudioUrlExtractor {
    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) {
        PlaybackBridge.play(url, autoPlay, isCache)
    }
}

/**
 * 自定义解析逻辑
 */
object AudioUrlCustomExtractor : AudioUrlExtractor {
    private var parse: ((String) -> String)? = null

    fun setUp(parse: (String) -> String) {
        this.parse = parse
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) {
        val p = parse
        if (p == null) {
            PlaybackBridge.play(url, autoPlay, isCache)
            return
        }
        extractorAsyncExecute(url, autoPlay, isCache, isDebug, backgroundTask = {
            try {
                p.invoke(url)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }, mainThreadCallback = { audioUrl ->
            if (audioUrl.isNotBlank()) {
                PlaybackBridge.play(audioUrl, autoPlay, isCache)
            }
        })
    }
}

/**
 * 音频地址在 json 里
 */
object AudioUrlJsonExtractor : AudioUrlExtractor {
    private var isDesktop = false
    private var parse: ((FuelJson) -> String)? = null

    @JvmOverloads
    fun setUp(isDesktop: Boolean = false, parse: (FuelJson) -> String) {
        this.isDesktop = isDesktop
        this.parse = parse
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) {
        val p = parse ?: return
        val desktop = isDesktop
        extractorAsyncExecute(url, autoPlay, isCache, isDebug, backgroundTask = {
            try {
                val ua = if (desktop) getDesktopUA() else getMobileUA()
                val result = Fuel.get(url).header(mapOf("User-Agent" to ua)).responseJson()
                p.invoke(result.third.get())
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }, mainThreadCallback = { audioUrl ->
            if (audioUrl.isNotBlank()) {
                PlaybackBridge.play(audioUrl, autoPlay, isCache)
            }
        })
    }
}

/**
 * 用 Jsoup 解析页面提取音频地址
 */
object AudioUrlJsoupExtractor : AudioUrlExtractor {
    private var isDesktop = false
    private var parse: ((Document) -> String)? = null

    @JvmOverloads
    fun setUp(isDesktop: Boolean = false, parse: (Document) -> String) {
        this.isDesktop = isDesktop
        this.parse = parse
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) {
        val p = parse ?: return
        val desktop = isDesktop
        extractorAsyncExecute(url, autoPlay, isCache, isDebug, backgroundTask = {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent(if (desktop) getDesktopUA() else getMobileUA())
                    .timeout(20000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .get()
                p.invoke(doc)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }, mainThreadCallback = { audioUrl ->
            if (audioUrl.isNotBlank()) {
                PlaybackBridge.play(audioUrl, autoPlay, isCache)
            }
        })
    }
}

/**
 * WebView 渲染页面后提取音频地址
 */
object AudioUrlWebViewExtractor : AudioUrlExtractor {
    private var isDesktop = false
    private var script: String = DEFAULT_HTML_SCRIPT
    private var parse: ((String) -> String?)? = null

    @JvmOverloads
    fun setUp(
        isDeskTop: Boolean = false,
        script: String = DEFAULT_HTML_SCRIPT,
        parse: (String) -> String?
    ) {
        this.isDesktop = isDeskTop
        this.script = script
        this.parse = parse
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) {
        val p = parse ?: return
        val desktop = isDesktop
        val js = script
        WebViewHelper.load(
            url = url,
            isDesktop = desktop,
            autoPlay = autoPlay,
            isCache = isCache,
            sniff = false,
            validateUrl = null,
            script = js,
            parseHtml = { raw -> p.invoke(raw) }
        )
    }
}

/**
 * WebView 自动嗅探音频地址
 */
object AudioUrlWebViewSniffExtractor : AudioUrlExtractor {
    private var isDesktop = false
    private var validateUrl: ((String) -> Boolean)? = null

    @JvmOverloads
    fun setUp(isDeskTop: Boolean = false, validateUrl: ((String) -> Boolean)? = null) {
        this.isDesktop = isDeskTop
        this.validateUrl = validateUrl
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) {
        WebViewHelper.load(
            url = url,
            isDesktop = isDesktop,
            autoPlay = autoPlay,
            isCache = isCache,
            sniff = true,
            validateUrl = validateUrl,
            script = DEFAULT_HTML_SCRIPT,
            parseHtml = null
        )
    }
}

/**
 * WebView 加载与提取的公共逻辑
 */
private object WebViewHelper {
    private const val TIMEOUT_MS = 20000L
    private const val RETRY_INTERVAL_MS = 600L
    private const val MAX_RETRY = 25

    @SuppressLint("SetJavaScriptEnabled")
    fun load(
        url: String,
        isDesktop: Boolean,
        autoPlay: Boolean,
        isCache: Boolean,
        sniff: Boolean,
        validateUrl: ((String) -> Boolean)?,
        script: String,
        parseHtml: ((String) -> String?)?
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(App.appContext)
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    userAgentString = if (isDesktop) getDesktopUA() else getMobileUA()
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                var finished = false
                var retryCount = 0
                val handler = Handler(Looper.getMainLooper())

                fun finish(audioUrl: String?) {
                    if (finished) return
                    finished = true
                    handler.removeCallbacksAndMessages(null)
                    try {
                        webView.stopLoading()
                        webView.destroy()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (!audioUrl.isNullOrBlank()) {
                        PlaybackBridge.play(audioUrl, autoPlay, isCache)
                    }
                }

                fun tryExtract() {
                    if (finished) return
                    webView.evaluateJavascript(script) { raw ->
                        if (finished) return@evaluateJavascript
                        val html = try {
                            org.apache.commons.lang3.StringEscapeUtils.unescapeJava(raw ?: "")
                        } catch (e: Exception) {
                            raw ?: ""
                        }
                        val audioUrl = try {
                            parseHtml?.invoke(html)
                        } catch (e: Exception) {
                            null
                        }
                        if (!audioUrl.isNullOrBlank()) {
                            finish(audioUrl)
                        } else {
                            retryCount++
                            if (retryCount < MAX_RETRY) {
                                handler.postDelayed({ tryExtract() }, RETRY_INTERVAL_MS)
                            } else {
                                finish(null)
                            }
                        }
                    }
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        super.onPageFinished(view, finishedUrl)
                        if (sniff) {
                            // 嗅探模式下，页面加载完成后也用 js 兜底找一次 audio 标签
                            retryCount = MAX_RETRY - 3
                        }
                        handler.postDelayed({ tryExtract() }, 300)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return null
                        if (sniff && isAudioUrl(reqUrl, validateUrl)) {
                            handler.post { finish(reqUrl) }
                        }
                        return null
                    }
                }

                handler.postDelayed({ finish(null) }, TIMEOUT_MS)
                webView.loadUrl(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isAudioUrl(url: String, validateUrl: ((String) -> Boolean)?): Boolean {
        if (validateUrl != null) {
            return try {
                validateUrl.invoke(url)
            } catch (e: Exception) {
                false
            }
        }
        val lower = url.lowercase()
        if (lower.startsWith("data:") || lower.startsWith("blob:")) return false
        return lower.contains(".mp3") || lower.contains(".m4a") || lower.contains(".aac") ||
                lower.contains(".ogg") || lower.contains(".wav") || lower.contains(".flac") ||
                lower.contains(".m3u8") || lower.contains("audio") && lower.contains(".mp")
    }
}
