package com.io.github.norbertweb.bluebird.browser.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.io.github.norbertweb.bluebird.browser.model.NewsArticle
import com.io.github.norbertweb.bluebird.browser.model.NewsSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small, network-conservative cache for the dynamic New Tab page.
 * Nothing polls in the background: content is refreshed only when the home
 * page is actually opened/resumed and the cache is older than its TTL.
 */
class HomeContentRepository private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("bluebird_home_content", Context.MODE_PRIVATE)
    private val cacheDir = File(context.cacheDir, "home_content").apply { mkdirs() }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        prefs.edit().remove("news_cache").remove("news_last_fetch").apply()
    }

    companion object {
        private const val NEWS_TTL_MS = 30 * 60 * 1000L
        private const val WALLPAPER_SLOT_MS = 5 * 60 * 60 * 1000L
        private const val MAX_NEWS = 18
        private const val MAX_SUMMARY = 180

        private val DEFAULT_SUBSCRIPTIONS = listOf(
            NewsSubscription("bbc-world", "BBC News", "https://feeds.bbci.co.uk/news/rss.xml"),
            NewsSubscription("bbc-tech", "BBC Technology", "https://feeds.bbci.co.uk/news/technology/rss.xml")
        )

        private val WALLPAPER_URLS = listOf(
            "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1600&q=82",
            "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1600&q=82",
            "https://images.unsplash.com/photo-1497250681960-ef046c08a56e?auto=format&fit=crop&w=1600&q=82",
            "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1600&q=82",
            "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1600&q=82",
            "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1600&q=82"
        )

        @Volatile private var instance: HomeContentRepository? = null
        fun get(context: Context): HomeContentRepository = instance ?: synchronized(this) {
            instance ?: HomeContentRepository(context.applicationContext).also { instance = it }
        }
    }

    fun loadSubscriptions(): List<NewsSubscription> {
        val raw = prefs.getString("subscriptions", null) ?: return DEFAULT_SUBSCRIPTIONS
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NewsSubscription(o.getString("id"), o.getString("name"), o.getString("feedUrl"), o.optBoolean("enabled", true))
            }
        }.getOrDefault(DEFAULT_SUBSCRIPTIONS)
    }

    fun saveSubscriptions(items: List<NewsSubscription>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("name", item.name); put("feedUrl", item.feedUrl); put("enabled", item.enabled)
            })
        }
        prefs.edit().putString("subscriptions", arr.toString()).apply()
    }

    suspend fun loadNews(force: Boolean = false): List<NewsArticle> = withContext(Dispatchers.IO) {
        val cached = readCachedNews()
        val lastFetch = prefs.getLong("news_last_fetch", 0L)
        if (!force && cached.isNotEmpty() && System.currentTimeMillis() - lastFetch < NEWS_TTL_MS) return@withContext cached

        val subscriptions = loadSubscriptions().filter { it.enabled }
        if (subscriptions.isEmpty()) return@withContext cached

        val fresh = subscriptions.flatMap { subscription -> fetchFeed(subscription) }
            .sortedByDescending { it.publishedAt }
            .distinctBy { it.url }
            .take(MAX_NEWS)

        if (fresh.isNotEmpty()) {
            writeCachedNews(fresh)
            prefs.edit().putLong("news_last_fetch", System.currentTimeMillis()).apply()
            fresh
        } else {
            cached
        }
    }

    fun currentWallpaperSlot(now: Long = System.currentTimeMillis()): Long = now / WALLPAPER_SLOT_MS

    suspend fun loadCurrentWallpaper(): Pair<Bitmap?, String> = withContext(Dispatchers.IO) {
        val slot = currentWallpaperSlot()
        val file = File(cacheDir, "wallpaper_$slot.jpg")
        val urlIndex = Math.floorMod(slot, WALLPAPER_URLS.size.toLong()).toInt()
        val url = WALLPAPER_URLS[urlIndex]

        val bitmap = if (file.exists() && file.length() > 0) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            downloadBitmap(url, file)
        }

        // Keep only the current and previous wallpaper cache entries.
        cacheDir.listFiles()?.filter { it.name.startsWith("wallpaper_") && it != file }
            ?.forEach { other ->
                val otherSlot = other.name.removePrefix("wallpaper_").removeSuffix(".jpg").toLongOrNull()
                if (otherSlot != null && otherSlot < slot - 1) other.delete()
            }

        bitmap to url
    }

    private fun downloadBitmap(urlString: String, target: File): Bitmap? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BluebirdBrowser/1.0")
        }
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            BufferedInputStream(connection.inputStream).use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return null
                FileOutputStream(target).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
                }
                bitmap
            }
        } catch (_: Exception) {
            target.delete()
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchFeed(subscription: NewsSubscription): List<NewsArticle> {
        val connection = (URL(subscription.feedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 9000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BluebirdBrowser/1.0 RSS reader")
        }
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) return emptyList()
            val parser = android.util.Xml.newPullParser().apply {
                setInput(connection.inputStream, "UTF-8")
            }
            val results = mutableListOf<NewsArticle>()
            var event = parser.eventType
            var insideItem = false
            var currentTitle = ""
            var currentLink = ""
            var currentDescription = ""
            var currentDate = 0L
            var currentTag = ""
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && results.size < 12) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()
                        if (currentTag == "item" || currentTag == "entry") {
                            insideItem = true
                            currentTitle = ""; currentLink = ""; currentDescription = ""; currentDate = 0L
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> if (insideItem) {
                        val value = parser.text?.trim().orEmpty()
                        when (currentTag) {
                            "title" -> currentTitle = value
                            "link" -> if (currentLink.isEmpty()) currentLink = value
                            "description", "summary" -> currentDescription = value
                            "pubdate", "published", "updated" -> currentDate = parseDate(value)
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        val tag = parser.name.lowercase()
                        if (tag == "item" || tag == "entry") {
                            if (currentTitle.isNotBlank() && currentLink.isNotBlank()) {
                                results += NewsArticle(
                                    id = "${subscription.id}:${currentLink.hashCode()}",
                                    source = subscription.name,
                                    title = cleanXmlText(currentTitle),
                                    url = currentLink.trim(),
                                    publishedAt = currentDate,
                                    summary = cleanXmlText(currentDescription).take(MAX_SUMMARY)
                                )
                            }
                            insideItem = false
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
            results
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDate(value: String): Long {
        val formats = listOf(
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US),
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm Z", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", java.util.Locale.US)
        )
        for (format in formats) runCatching { return format.parse(value)?.time ?: 0L }
        return 0L
    }

    private fun cleanXmlText(value: String): String = value
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("&(?:amp|lt|gt|quot|apos);")) { entity ->
            mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'")[entity.value] ?: entity.value
        }
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun readCachedNews(): List<NewsArticle> {
        val raw = prefs.getString("news_cache", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NewsArticle(o.getString("id"), o.getString("source"), o.getString("title"), o.getString("url"), o.optLong("publishedAt", 0L), o.optString("summary", ""))
            }
        }.getOrDefault(emptyList())
    }

    private fun writeCachedNews(items: List<NewsArticle>) {
        val arr = JSONArray()
        items.take(MAX_NEWS).forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("source", item.source); put("title", item.title); put("url", item.url)
                put("publishedAt", item.publishedAt); put("summary", item.summary)
            })
        }
        prefs.edit().putString("news_cache", arr.toString()).apply()
    }
}
