package com.github.eprendre.sources_by_eprendre

import com.github.eprendre.tingshu.sources.TingShu

object SourceEntry {

    /**
     * 说明
     */
    @JvmStatic
    fun getDesc(): String {
        return "听书源"
    }

    /**
     * 获取内容分类标识（如"听书"）。
     *
     * 相同分类名称（如"听书"）用于聚合不同订阅源的内容，
     * 使用户能够统一浏览或搜索同一分类下的所有内容。
     *
     * @return 当前内容的分类名称，例如"听书"、"视频"或"音乐"等
     */
    @JvmStatic
    fun getCategory(): String {
        return "听书"
    }

    /**
     * 返回此包下面的源, 若编译不通过请删除下面不存在的源。
     *
     * 2026-10 说明：原来注册的 30+ 个站点大部分已经关停、改版或改成影视站，
     * 调用后只会得到空白页，因此这里只保留经实测仍可正常「搜索 / 分类 / 播放」的站点。
     * 其余源文件仍保留在源码里，确认恢复后可重新加回本列表。
     */
    @JvmStatic
    fun getSources(): List<TingShu> {
        HuanTingWang.c24e329b36b542f4adde9694d4b28a4r()
        return listOf(
            KuWo,          // 酷我畅听：搜索 + 分类 + 章节 + 直链播放均正常
            BiliBili,      // 哔哩哔哩：搜索可用
            JiHe,          // 机核 GCORES：电台 / 播单
            VBus,          // 声音巴士：最新 / 最热 / 推荐
            LibriVox       // LibriVox：公版英文有声书
        )
    }
}