package com.github.eprendre.tingshu.sources

import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.eprendre.sources_by_eprendre.SourceEntry
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.getBitmapFromVectorDrawable
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.utils.Section
import com.github.eprendre.tingshu.utils.SectionTab
import com.github.eprendre.tingshu.widget.GlideApp
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 书源调度中心：把 30+ 个书源统一暴露给界面层。
 */
object TingShuSourceHandler {

    /** 全部内置书源 */
    val allSources: List<TingShu> by lazy { SourceEntry.getSources() }

    private var currentSource: TingShu = allSources.first()

    /** 首页分类缓存，避免在主线程请求网络 */
    @Volatile
    private var mainSectionsCache: List<SectionTab>? = null

    private val loadLatch = CountDownLatch(1)

    init {
        setupConfig()
        preloadSections()
    }

    /**
     * 从配置里恢复上次选择的源
     */
    fun setupConfig() {
        val id = Prefs.source
        currentSource = allSources.firstOrNull { it.getSourceId() == id } ?: allSources.first()
    }

    fun getCurrentSource(): TingShu = currentSource

    fun findSourceById(id: String?): TingShu? {
        if (id.isNullOrBlank()) return null
        return allSources.firstOrNull { it.getSourceId() == id }
    }

    /**
     * 根据链接判断属于哪个源
     */
    fun findSource(url: String): TingShu {
        return allSources.firstOrNull { it.getUrl().isNotBlank() && url.startsWith(it.getUrl()) }
            ?: currentSource
    }

    /**
     * 后台预加载首页分类
     */
    fun preloadSections() {
        Schedulers.io().scheduleDirect {
            try {
                mainSectionsCache = computeMainSections()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadLatch.countDown()
            }
        }
    }

    private fun computeMainSections(): List<SectionTab> {
        return currentSource.getCategoryMenus().flatMap { menu ->
            menu.tabs.map { tab -> SectionTab("${menu.title}·${tab.title}", tab.url) }
        }
    }

    /**
     * 首页分类：把当前源的分类菜单展平成标签页
     * 首次调用会短暂等待后台预加载完成（最多 2 秒），避免在主线程发起网络请求。
     */
    fun getMainSections(): List<SectionTab> {
        mainSectionsCache?.let { return it }
        try {
            loadLatch.await(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        mainSectionsCache?.let { return it }
        preloadSections()
        return emptyList()
    }

    fun getOtherSections(): List<SectionTab> = emptyList()

    fun getSectionDetail(url: String): Single<Section> {
        return Single.fromCallable {
            try {
                findSource(url).getCategoryList(url)
            } catch (e: Exception) {
                e.printStackTrace()
                Section(emptyList(), 1, 1, url, "")
            }
        }.subscribeOn(Schedulers.io())
    }

    /**
     * 搜索（使用当前选中的源）
     */
    fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        val source = currentSource
        return Single.fromCallable {
            try {
                source.search(keywords, page)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(emptyList(), 1)
            }
        }.subscribeOn(Schedulers.io())
    }

    /**
     * 加载某本书的全部章节到播放列表
     */
    fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val source = findSource(bookUrl)
            val detail = source.getBookDetailInfo(bookUrl, true, true)

            App.playList = detail.playList.map { Episode(it.title, it.url) }

            val book = App.currentBook?.takeIf { it.bookUrl == bookUrl }
            if (book != null) {
                if (book.title.isNotBlank()) Prefs.currentBookName = book.title
                if (book.coverUrl.isNotBlank()) Prefs.currentCover = book.coverUrl
                if (book.author.isNotBlank()) Prefs.author = book.author
                if (book.artist.isNotBlank()) Prefs.artist = book.artist
            }
            if (detail.artist.isNotBlank()) Prefs.artist = detail.artist
            if (detail.author.isNotBlank() && Prefs.author.isNullOrBlank()) {
                Prefs.author = detail.author
            }
            if (detail.coverUrl.isNotBlank() && Prefs.currentCover.isNullOrBlank()) {
                Prefs.currentCover = detail.coverUrl
            }

            downloadCoverForNotification()
            null
        }.subscribeOn(Schedulers.io())
    }

    /**
     * 取某个链接对应的音频提取器
     */
    fun getAudioUrlExtractor(url: String): AudioUrlExtractor {
        val source = findSource(url)
        try {
            source.reset()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return source.getAudioUrlExtractor()
    }

    /**
     * 播放之前先调用这个方法确保当前通知所需要的封面已缓存
     */
    fun downloadCoverForNotification() {
        val glideOptions = RequestOptions()
            .error(R.drawable.ic_launcher_background)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        try {
            App.coverBitmap = GlideApp.with(App.appContext)
                .applyDefaultRequestOptions(glideOptions)
                .asBitmap()
                .load(Prefs.currentCover)
                .submit(144, 144)
                .get()
        } catch (e: Exception) {
            e.printStackTrace()
            App.coverBitmap = getBitmapFromVectorDrawable(App.appContext, R.drawable.ic_launcher_background)
        }
    }
}
