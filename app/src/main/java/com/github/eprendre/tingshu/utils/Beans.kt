package com.github.eprendre.tingshu.utils

import androidx.annotation.Keep
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节
 */
@Keep
data class Episode(val title: String, val url: String) {
    var isFree: Boolean = true
    var isCached: Boolean = false
    var progress: Int = 0
    var coverUrl: String = ""
    var transcript: String = ""

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url && oldItem.title == newItem.title
            }
        }
    }
}

/**
 * 书籍。既是书源返回的数据模型，也是收藏用的数据库实体。
 */
@Keep
@Entity(tableName = "my_books", indices = [Index(value = ["book_url"], unique = true)])
data class Book(
    @ColumnInfo(name = "cover_url") var coverUrl: String,
    @ColumnInfo(name = "book_url") val bookUrl: String,
    var title: String,
    var author: String,
    var artist: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
    var intro: String = ""
    var currentEpisodeUrl: String? = null
    var currentEpisodeName: String? = null
    var currentEpisodePosition: Long = 0

    var skipBeginning: Long = 0
    var skipEnd: Long = 0
    var volumeBoostLevel: Int = 0
    var playOrderType: Int = 0
    var playSpeed: Float = 1f
    var isFree: Boolean = true
    var isEpisodesReversed: Boolean = false

    @Ignore
    var episodeList: List<Episode>? = null

    var hasFullEpisodes: Boolean = false
    var isShowBriefChapterTitle: Boolean = false
    var sourceId: String? = null
    var status: String = ""
    var episodesUpdateTime: Long = 0
    var isTransientEpisodes: Boolean = false
    var isCompleted: Boolean = false

    fun copyFrom(book: Book) {
        this.coverUrl = book.coverUrl
        this.title = book.title
        this.author = book.author
        this.artist = book.artist
        this.intro = book.intro
        this.status = book.status
        this.sourceId = book.sourceId
        this.isCompleted = book.isCompleted
        if (book.episodeList != null) {
            this.episodeList = book.episodeList
        }
        this.currentEpisodeUrl = book.currentEpisodeUrl
        this.currentEpisodeName = book.currentEpisodeName
        this.currentEpisodePosition = book.currentEpisodePosition
        this.skipBeginning = book.skipBeginning
        this.skipEnd = book.skipEnd
        this.playSpeed = book.playSpeed
        this.playOrderType = book.playOrderType
        this.volumeBoostLevel = book.volumeBoostLevel
        this.isEpisodesReversed = book.isEpisodesReversed
        this.hasFullEpisodes = book.hasFullEpisodes
        this.isShowBriefChapterTitle = book.isShowBriefChapterTitle
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Book) return false
        return bookUrl == other.bookUrl
    }

    override fun hashCode(): Int {
        return bookUrl.hashCode()
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl &&
                        oldItem.currentEpisodeUrl == newItem.currentEpisodeUrl &&
                        oldItem.currentEpisodePosition == newItem.currentEpisodePosition
            }
        }
    }
}

data class BookDetail(
    val playList: List<Episode>,
    val intro: String? = "",
    val artist: String = "",
    val author: String = "",
    val episodesCount: Int = 0,
    val coverUrl: String = ""
) {
    val title: String = ""
}

interface IMenu {
    fun getType(): Int
}

/**
 * 分类下的子分类
 */
data class CategoryTab(
    val title: String,
    val url: String
) : IMenu {
    override fun getType(): Int {
        return TYPE_TAB
    }

    companion object {
        const val TYPE_TAB = 0
    }
}

/**
 * 大分类
 */
data class CategoryMenu(
    val title: String,
    val tabs: List<CategoryTab>
) : IMenu {
    override fun getType(): Int {
        return TYPE_MENU
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CategoryMenu) return false
        return title == other.title
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }

    companion object {
        const val TYPE_MENU = 1
    }
}

/**
 * 分类列表页
 */
data class Category(
    val list: List<Book>,
    val currentPage: Int,
    val totalPage: Int,
    val currentUrl: String,
    val nextUrl: String
)

/**
 * 供自定义源配置文件使用
 */
sealed class ConfigItem(
    val key: String,
    val label: String
) {
    class Button(label: String, val click: () -> Unit) : ConfigItem("", label)
    class Text(key: String, label: String, var default: String = "") : ConfigItem(key, label)
    class Select(key: String, label: String, val options: List<String>, var default: String) : ConfigItem(key, label)
    class Switch(key: String, label: String, var default: Boolean) : ConfigItem(key, label)
    class MultiSelect(key: String, label: String, val options: List<String>, var default: List<String> = listOf()) : ConfigItem(key, label)
}

// ===================== 兼容旧版 UI 的类型别名 =====================
// 旧版界面代码使用 SectionTab / Section 两个名字，这里做别名，界面代码无需改动。
typealias SectionTab = CategoryTab
typealias Section = Category
