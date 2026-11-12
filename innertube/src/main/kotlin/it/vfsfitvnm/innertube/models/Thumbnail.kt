package it.vfsfitvnm.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnail(
    val url: String,
    val height: Int?,
    val width: Int?
) {
    val isResizable: Boolean
        get() = !url.startsWith("https://i.ytimg.com")

    fun size(size: Int): String {
        return when {
            url.startsWith("https://lh3.googleusercontent.com") ||
                    url.startsWith("https://yt3.ggpht.com") ||
                    url.startsWith("https://yt3.googleusercontent.com") -> {
                val queryStart = url.indexOfAny(charArrayOf('?', '#')).let { if (it < 0) url.length else it }
                val path = url.substring(0, queryStart).substringBeforeLast('=')
                path + "=w$size-h$size" + url.substring(queryStart)
            }
            else -> url
        }
    }
}

fun List<Thumbnail>?.bestResolution(): Thumbnail? = this?.maxByOrNull {
    (it.width?.toLong() ?: 0L) * (it.height?.toLong() ?: 0L)
}
