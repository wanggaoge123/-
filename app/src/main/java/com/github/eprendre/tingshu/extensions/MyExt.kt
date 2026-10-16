package com.github.eprendre.tingshu.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.widget.PlaybackBridge
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jsoup.Connection
import java.io.File
import java.net.URL
import java.net.URLDecoder

// ===================== 图形相关 =====================

fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
    var drawable = ContextCompat.getDrawable(context, drawableId)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = DrawableCompat.wrap(drawable!!).mutate()
    }

    val bitmap = Bitmap.createBitmap(
        drawable!!.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

// ===================== 书源可用的宿主 API =====================

/**
 * 默认 PC 版 UA
 */
private const val UA_DESKTOP =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/**
 * 默认手机版 UA
 */
private const val UA_MOBILE =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

fun splitQuery(url: URL): LinkedHashMap<String, String> {
    val queryPairs = LinkedHashMap<String, String>()
    val query = url.query
    val pairs = query.split("&")
    for (pair in pairs) {
        val idx = pair.indexOf("=")
        if (idx < 0) continue
        queryPairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] =
            URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
    }
    return queryPairs
}

/**
 * 是否使用 PC 版的 UA
 */
fun Connection.config(isDesktop: Boolean = false): Connection {
    return this.userAgent(if (isDesktop) getDesktopUA() else getMobileUA())
        .timeout(20000)
        .ignoreContentType(true)
        .ignoreHttpErrors(true)
}

fun getDesktopUA(): String = UA_DESKTOP

fun getMobileUA(): String = UA_MOBILE

/**
 * WebView 登录之后，可以用此方法得到 Cookie
 */
fun getCookie(url: String): String? {
    return try {
        CookieManager.getInstance().getCookie(url)
    } catch (e: Exception) {
        null
    }
}

/**
 * 提示消息
 */
fun showToast(msg: String) {
    Handler(Looper.getMainLooper()).post {
        try {
            Toast.makeText(App.appContext, msg, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 获取当前正在播放的书籍
 */
fun getCurrentBook(): Book? {
    return App.currentBook
}

/**
 * 加载多页章节列表时的进度提示（本 App 静默处理）
 */
fun notifyLoadingEpisodes(pageInfo: String?) {
    // 章节过多时源会分页加载，这里不做界面提示，避免打扰。
}

/**
 * 后台任务 -> 主线程回调
 */
fun extractorAsyncExecute(
    url: String,
    autoPlay: Boolean,
    isCache: Boolean,
    isDebug: Boolean,
    backgroundTask: () -> String,
    mainThreadCallback: (String) -> Unit
): Disposable {
    return Single.fromCallable { backgroundTask() }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ result ->
            try {
                mainThreadCallback(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { e ->
            e.printStackTrace()
        })
}

/**
 * 获取今天播放时长, 单位毫秒
 */
fun todayPlaybackTime(): Long {
    return App.todayPlaybackTime
}

/**
 * 控制播放器
 */
fun playerControl(action: String) {
    PlaybackBridge.control(action)
}

/**
 * 获取每个源自己的缓存目录
 */
fun getSourceCacheDir(sourceId: String, subDir: String): File {
    val base = App.appContext.getExternalFilesDir(null) ?: App.appContext.filesDir
    val dir = File(File(File(base, "sources"), sourceId), subDir)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}
