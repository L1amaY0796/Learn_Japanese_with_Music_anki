package com.learn_japanese_with_music.core.utils

import android.util.Log
import androidx.annotation.Keep
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
@Keep
object ShareSearch {
    private val client = OkHttpClient()

    /**
     * 從分享文字中提取初始資訊。
     * 如果是 Spotify 的「歌名 · 歌手 · 連結」格式，直接回傳「歌名 歌手」。
     * 否則回傳原始文字（可能是網址）。
     */
    fun extractInitialData(text: String): String {
        // Spotify 標準格式: "Song · Artist · Link"
        val parts = text.split(" · ")
        if (parts.size >= 2) {
            return "${parts[0]} ${parts[1]}"
        }
        
        // 移除可能的網址以便檢查是否還有文字
        val urlRegex = "(https?://\\S+)".toRegex()
        val textWithoutUrl = text.replace(urlRegex, "").trim()
        
        return if (textWithoutUrl.isNotEmpty()) {
            textWithoutUrl
        } else {
            text.trim() // 只有網址，回傳原始網址供後續解析
        }
    }

    /**
     * 判定字串是否為網址
     */
    fun isUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://") || text.contains("spotify.link")
    }

    /**
     * 解析網址中的 Metadata 並轉換為搜尋關鍵字
     */
    fun resolveUrlToQuery(url: String): String? {
        return when {
            url.contains("spotify.com") || url.contains("spotify.link") -> resolveSpotifyUrl(url)
            else -> resolveGeneralUrl(url)
        }
    }

    private fun resolveSpotifyUrl(url: String): String? {
        return try {
            // 使用 Spotify 公開的 OEmbed 介面，不需要 Token
            val oembedUrl = "https://open.spotify.com/oembed?url=$url"
            val request = Request.Builder().url(oembedUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val json = JSONObject(bodyString)
                val title = json.optString("title")
                val artist = json.optString("author_name")
                if (title.isNotEmpty() && artist.isNotEmpty()) "$title $artist" else title
            }
        } catch (e: Exception) {
            Log.e("ShareSearch", "Spotify OEmbed failed", e)
            null
        }
    }

    private fun resolveGeneralUrl(url: String): String? {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()
            
            // 1. 優先找 OpenGraph 標籤 (音樂/影片 App 通常會設定)
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            if (ogTitle.isNotEmpty()) {
                return ogTitle
            }
            
            // 2. 退而求其次抓取網頁標題
            doc.title()
        } catch (e: Exception) {
            Log.e("ShareSearch", "General URL resolution failed", e)
            null
        }
    }
}
