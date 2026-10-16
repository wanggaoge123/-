package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.extensions.getMobileUA
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.BookDetail
import com.github.eprendre.tingshu.utils.Category
import com.github.eprendre.tingshu.utils.CategoryMenu
import com.github.eprendre.tingshu.utils.ConfigItem

/**
 * 书源基类。全部书源都继承此类。
 */
abstract class TingShu {
    /**
     * 用来判断一本书属于哪个源，不能和其它源有重复。
     */
    abstract fun getSourceId(): String

    /**
     * 需要提供合法的网址，有时候用来判断链接是否属于某个源。
     */
    abstract fun getUrl(): String

    /**
     * 站点名称
     */
    abstract fun getName(): String

    /**
     * 站点简介
     */
    open fun getDesc() = ""

    /**
     * 返回 Pair 的第二个 Int 参数为最大页数，用来判断是否有下一页。
     */
    abstract fun search(keywords: String, page: Int): Pair<List<Book>, Int>

    /**
     * 选择合适的音频提取逻辑
     */
    abstract fun getAudioUrlExtractor(): AudioUrlExtractor

    /**
     * 分类菜单
     */
    abstract fun getCategoryMenus(): List<CategoryMenu>

    /**
     * 分类页面的列表
     */
    abstract fun getCategoryList(url: String): Category

    /**
     * 当前站点的章节列表是否需要分页加载
     */
    open fun isMultipleEpisodePages() = false

    /**
     * 获取书籍详情（含章节列表）
     */
    abstract fun getBookDetailInfo(bookUrl: String, loadEpisodes: Boolean = true, loadFullPages: Boolean = true): BookDetail

    /**
     * 翻页加载是耗时操作，如果此方法触发，需要取消掉翻页加载的操作。
     */
    open fun reset() = run { }

    /**
     * 当前源是否可搜索
     */
    open fun isSearchable() = true

    /**
     * 当前源是否有发现分类的相关内容
     */
    open fun isDiscoverable() = true

    /**
     * 当前源的音频地址是否能被缓存
     */
    open fun isCacheable() = true

    /**
     * 当前源的解析是否没有用到 WebView
     */
    open fun isWebViewNotRequired() = false

    /**
     * WebView 使用兼容模式运行
     */
    open fun isWebViewCompatible() = false

    open fun method238() = false

    open fun c24e329b36b542f4adde9694d4b28a4r() = false
}

interface AudioUrlExtractor {
    fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean = false)
}

/**
 * 请求音频地址时是否需要额外的 header
 */
interface AudioUrlExtraHeaders {
    fun headers(audioUrl: String): Map<String, String>
}

/**
 * 加载封面时是否需要额外的 headers
 */
interface CoverUrlExtraHeaders {
    fun coverHeaders(coverUrl: String, headers: MutableMap<String, String>): Boolean
}

/**
 * 当源需要登录才可以获取数据时实现此接口
 */
interface ILogin {
    fun getLoginUrl(): String

    fun isLoginDesktop(): Boolean = false
}

/**
 * 播放状态有变化时的回调
 */
interface IPlaybackStateCallback {
    /**
     * state 参数有：playing, paused, error, stopped
     */
    fun onPlaybackStateChanged(state: String)
}

/**
 * 歌词接口
 */
interface ILrc {
    fun saveLrcConfig(hashMap: HashMap<String, String>, sourceId: String, bookUrl: String) {
        // 本 App 暂不支持歌词，留空实现
    }

    fun downloadLrc(url: String): String
}

/**
 * 搜索验证接口
 */
interface ISearchVerification {
    fun getSearchVerificationUrl(keywords: String): String

    fun isSearchValidated(): Boolean = false

    fun getSearchVerificationUA(): String = getMobileUA()

    fun getSearchDelayMs(): Long = 0
}

/**
 * 自定义源配置
 */
interface ConfigurableSource {
    fun getCustomConfigItems(): List<ConfigItem>
}
