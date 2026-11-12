package it.vfsfitvnm.innertube.requests

import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.io.IOException

private class StreamResolverDownloader : Downloader() {
    private val client = OkHttpClient()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val builder = OkHttpRequest.Builder()
            .url(request.url())
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
            )

        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { value -> builder.addHeader(name, value) }
        }

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) throw ReCaptchaException("YouTube requested a captcha", request.url())

            val body = response.body?.string()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                body,
                response.request.url.toString()
            )
        }
    }
}

internal object NewPipeStreamUrl {
    init {
        NewPipe.init(StreamResolverDownloader())
    }

    fun resolve(videoId: String, url: String?, signatureCipher: String?): String? = runCatching {
        val streamUrl = url ?: signatureCipher?.let { cipher ->
            val parameters = parseQueryString(cipher)
            val signature = parameters["s"]
                ?: throw ParsingException("Missing cipher signature")
            val signatureParameter = parameters["sp"]
                ?: throw ParsingException("Missing cipher signature parameter")
            val sourceUrl = parameters["url"]?.let(::URLBuilder)
                ?: throw ParsingException("Missing cipher URL")

            sourceUrl.parameters[signatureParameter] =
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, signature)
            sourceUrl.buildString()
        } ?: return null

        YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, streamUrl)
    }.getOrNull()
}
