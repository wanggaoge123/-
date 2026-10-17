package com.github.eprendre.sources_by_eprendre

import com.github.eprendre.tingshu.sources.AudioUrlDirectExtractor
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.ceil

/**
 * 酷我畅听
 *
 * 2026-10 维护说明：
 *  1. 搜索：baby.kuwo.cn/tingshu/api/search/Search（返回 albumId 为字符串、total 为字符串）
 *  2. 分类：filter/albums 与 page/boutique/getBoutiqueData（返回 albumId 为整数、total 为整数）
 *     两处字段类型不一致，因此统一用 optString / optInt 容错读取。
 *  3. 专辑曲目：原 /tingshu/api/data/album/songs 已下线（404），
 *     改用 search.kuwo.cn/r.s?stype=albuminfo，一次返回全部曲目（name + musicrid）。
 *  4. 播放：antiserver.kuwo.cn/anti.s 302 跳到真实 mp3，直链可用。
 */
object KuWo : TingShu() {

    private const val WEB_BASE = "https://baby.kuwo.cn/"
    private const val SEARCH_API = "https://baby.kuwo.cn/tingshu/api/search/Search"
    private const val BOUTIQUE_API = "https://baby.kuwo.cn/tingshu/api/page/boutique/getBoutiqueData"
    private const val FILTER_API = "https://baby.kuwo.cn/tingshu/api/filter/albums"
    private const val ALBUM_INFO_API =
        "https://search.kuwo.cn/r.s?stype=albuminfo&vipver=1&encoding=utf8&rformat=json&mobi=1&albumid="
    private const val AUDIO_API =
        "http://antiserver.kuwo.cn/anti.s?useless=/resource/&format=mp3&response=res&type=convert_url&rid=MUSIC_"

    override fun getSourceId(): String {
        return "502efedf0613460a9967d9e86ce2b24c"
    }

    override fun getUrl(): String {
        return WEB_BASE
    }

    override fun getName(): String {
        return "酷我畅听"
    }

    override fun getDesc(): String {
        return "推荐指数:5星 ⭐⭐⭐⭐⭐\n在线音频聚合，小说、评书、相声、人文历史齐全，支持搜索。"
    }

    override fun isWebViewNotRequired(): Boolean {
        return true
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor {
        return AudioUrlDirectExtractor
    }

    /** 统一生成专辑详情地址：仍然挂在 baby.kuwo.cn 下，便于 findSource() 归属到本源的域名。 */
    private fun albumPageUrl(albumId: String): String {
        return "${WEB_BASE}tingshu/api/data/album/songs?albumId=$albumId&online=0"
    }

    private fun albumIdOf(bookUrl: String): String {
        return Regex("albumId=(\\d+)").find(bookUrl)?.groupValues?.get(1)
            ?: Regex("(\\d{3,})").find(bookUrl)?.groupValues?.get(1)
            ?: ""
    }

    /** 搜索接口的条目（albumId/total/songTotal 均为字符串） */
    private fun bookFromSearchItem(item: JSONObject): Book? {
        val albumId = item.optString("albumId").ifBlank { return null }
        val title = item.optString("albumName").ifBlank { item.optString("name") }
        if (title.isBlank()) return null
        val cover = item.optString("coverImg").ifBlank { item.optString("img") }
        val artist = item.optString("artistName")
        val total = item.optInt("songTotal")
        val intro = item.optString("title")
        return Book(cover, albumPageUrl(albumId), title, "", if (artist.isBlank()) "" else "播音: $artist").apply {
            this.status = if (total > 0) "共 $total 章" else ""
            this.intro = intro
            this.sourceId = getSourceId()
        }
    }

    /** 分类接口的条目（albumId/total/songTotal 均为整数，封面字段为 coverImg 或 img） */
    private fun bookFromAlbumItem(item: JSONObject): Book? {
        val albumId = item.optString("albumId").ifBlank { return null }
        val title = item.optString("albumName").ifBlank { item.optString("name") }
        if (title.isBlank()) return null
        val cover = item.optString("coverImg").ifBlank { item.optString("img") }
        val artist = item.optString("artistName")
        val total = item.optInt("songTotal")
        val intro = item.optString("title")
        return Book(cover, albumPageUrl(albumId), title, "", if (artist.isBlank()) "" else "播音: $artist").apply {
            this.status = if (total > 0) "共 $total 章" else ""
            this.intro = intro
            this.sourceId = getSourceId()
        }
    }

    override fun search(keywords: String, page: Int): Pair<List<Book>, Int> {
        val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
        val url = "$SEARCH_API?rn=10&type=album&version=8.5.6.1&kweexVersion=1.0.2" +
                "&wd=$encodedKeywords&pn=$page"

        val root = Fuel.get(url).responseJson().third.get().obj()
        val data = root.optJSONObject("data") ?: return Pair(emptyList(), 1)
        val total = data.optInt("total")
        val pageCount = if (total <= 0) 1 else ceil(total.toFloat() / 10).toInt()

        val list = ArrayList<Book>()
        val array = data.optJSONArray("data") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            bookFromSearchItem(item)?.let { list.add(it) }
        }
        return Pair(list, pageCount)
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        fun filter(title: String, classifyId: Int, categoryId: Int) = CategoryTab(
            title,
            "$FILTER_API?sortType=tsScore&classifyId=$classifyId&rn=20&categoryId=$categoryId&pn=1&kweexVersion=1.0.2"
        )

        val menu1 = CategoryMenu(
            "小说", listOf(
                CategoryTab("付费排行", "$BOUTIQUE_API?pt=1&rn=100&version=8.5.6.1&pn=1&kweexVersion=1.0.2"),
                CategoryTab("免费排行", "$BOUTIQUE_API?pt=2&rn=100&version=8.5.6.1&pn=1&kweexVersion=1.0.2"),
                filter("都市传说", 42, 2),
                filter("玄幻奇幻", 44, 2),
                filter("悬疑推理", 45, 2),
                filter("现代言情", 41, 2),
                filter("武侠仙侠", 48, 2),
                filter("穿越架空", 52, 2),
                filter("经典小说", 64, 2),
                filter("青春校园", 55, 2),
                filter("历史军事", 56, 2),
                filter("科幻竞技", 57, 2),
                filter("古代言情", 207, 2)
            )
        )
        val menu2 = CategoryMenu(
            "人文", listOf(
                filter("能力提升", 82, 4),
                filter("人文艺术", 77, 4),
                filter("国学文化", 78, 4),
                filter("成功法则", 79, 4),
                filter("外语精通", 76, 4),
                filter("养生健康", 81, 4),
                filter("酷我读书", 211, 4)
            )
        )
        val menu3 = CategoryMenu(
            "历史", listOf(
                filter("国学经典", 117, 9),
                filter("历史小说", 181, 9),
                filter("纪实档案", 118, 9),
                filter("历史传奇", 119, 9),
                filter("人物传奇", 120, 9),
                filter("文化讲堂", 121, 9),
                filter("百家讲坛", 212, 9)
            )
        )
        return listOf(menu1, menu2, menu3)
    }

    override fun getCategoryList(url: String): Category {
        val currentPage = Regex("[?&]pn=(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val pageSize = Regex("[?&]rn=(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 20

        val root = Fuel.get(url).responseJson().third.get().obj()
        val data = root.optJSONObject("data") ?: JSONObject()

        var pageCount = 1
        if (data.has("pageInfo")) {
            // 精品页的 pageInfo 形如 {pageCount:"1", total:2091, pageSize:"100"}，
            // total 是全站总量，不能直接除，优先用 pageCount。
            val pageInfo = data.optJSONObject("pageInfo")
            val declared = pageInfo?.optInt("pageCount") ?: 0
            val size = pageInfo?.optInt("pageSize") ?: 0
            val total = pageInfo?.optInt("total") ?: 0
            pageCount = when {
                declared > 0 -> declared
                total > 0 && size > 0 -> ceil(total.toFloat() / size).toInt()
                else -> 1
            }
        } else {
            val total = data.optInt("total")
            if (total > 0) pageCount = ceil(total.toFloat() / pageSize).toInt()
        }
        if (pageCount < 1) pageCount = 1

        val nextUrl = if (currentPage < pageCount) {
            url.replace(Regex("pn=(\\d+)"), "pn=${currentPage + 1}")
        } else {
            ""
        }

        val list = ArrayList<Book>()
        val topDatas = data.optJSONArray("topDatas")
        if (topDatas != null && topDatas.length() > 0) {
            for (i in 0 until topDatas.length()) {
                val wrapper = topDatas.optJSONObject(i) ?: continue
                val album = wrapper.optJSONObject("albums")
                if (album != null) {
                    bookFromAlbumItem(album)?.let { list.add(it) }
                } else {
                    // 部分栏目直接平铺专辑字段
                    bookFromAlbumItem(wrapper)?.let { list.add(it) }
                }
            }
        } else {
            val array = data.optJSONArray("data") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                bookFromAlbumItem(item)?.let { list.add(it) }
            }
        }

        return Category(list, currentPage, pageCount, url, nextUrl)
    }

    override fun getBookDetailInfo(bookUrl: String, loadEpisodes: Boolean, loadFullPages: Boolean): BookDetail {
        val episodes = ArrayList<Episode>()
        if (!loadEpisodes) return BookDetail(episodes)

        val albumId = albumIdOf(bookUrl)
        if (albumId.isBlank()) return BookDetail(episodes)

        val root = Fuel.get(ALBUM_INFO_API + albumId).responseJson().third.get().obj()
        val musicList = root.optJSONArray("musiclist") ?: JSONArray()
        for (i in 0 until musicList.length()) {
            val item = musicList.optJSONObject(i) ?: continue
            val name = item.optString("name").ifBlank { item.optString("songname") }
            var rid = item.optString("musicrid")
            if (rid.isBlank()) rid = item.optString("id")
            if (name.isBlank() || rid.isBlank()) continue
            episodes.add(Episode(name, AUDIO_API + rid))
        }
        return BookDetail(episodes)
    }
}
