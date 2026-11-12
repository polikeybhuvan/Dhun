package it.vfsfitvnm.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.Context
import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.utils.runCatchingNonCancellable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private suspend fun Innertube.fetchPlayerResponse(
    body: PlayerBody,
    context: Context
) = client.post(player) {
    setBody(body.copy(context = context))
    mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId")
}.body<PlayerResponse>()

private fun PlayerResponse.resolveStreamUrls(videoId: String): PlayerResponse = copy(
    streamingData = streamingData?.copy(
        adaptiveFormats = streamingData.adaptiveFormats?.map { format ->
            format.copy(
                url = NewPipeStreamUrl.resolve(
                    videoId = videoId,
                    url = format.url,
                    signatureCipher = format.signatureCipher
                ) ?: format.url
            )
        }
    )
)

private val PlayerResponse.hasPlayableAudio: Boolean
    get() = playabilityStatus?.status == "OK" && streamingData?.adaptiveFormats
        ?.any { it.mimeType.startsWith("audio/") && it.url != null } == true

suspend fun Innertube.player(body: PlayerBody) = runCatchingNonCancellable {
    val visitorData = runCatching {
        val response = client.get("https://music.youtube.com/sw.js_data").bodyAsText()
        Json.parseToJsonElement(response.drop(5))
            .jsonArray[0].jsonArray[2].jsonArray
            .filterIsInstance<JsonPrimitive>()
            .firstOrNull { it.contentOrNull?.startsWith("Cgt") == true }
            ?.jsonPrimitive?.content
    }.getOrNull() ?: Context.DefaultAndroid.client.visitorData

    val embeddedContext = Context.DefaultWebEmbeddedPlayer.copy(
        thirdParty = Context.ThirdParty("https://www.youtube.com/watch?v=${body.videoId}")
    )
    val contexts = listOf(
        Context.DefaultVisionOS.copy(
            client = Context.DefaultVisionOS.client.copy(visitorData = visitorData)
        ),
        embeddedContext,
        body.context,
        Context.DefaultAgeRestrictionBypass.copy(
            thirdParty = Context.ThirdParty("https://www.youtube.com/watch?v=${body.videoId}")
        )
    ).distinct()

    var bestResponse: PlayerResponse? = null
    for (context in contexts) {
        val response = runCatching { fetchPlayerResponse(body, context) }.getOrNull() ?: continue

        if (response.videoDetails?.videoId?.let { it != body.videoId } == true) continue

        val resolvedResponse = response.resolveStreamUrls(body.videoId)
        if (resolvedResponse.hasPlayableAudio) return@runCatchingNonCancellable resolvedResponse

        // A restriction response from one client context can be transient. Keep a
        // more useful response from another context instead of always returning
        // the first response (which may incorrectly block playback).
        val previousBest = bestResponse
        if (previousBest == null ||
            (previousBest.playabilityStatus?.status == "LOGIN_REQUIRED" &&
                resolvedResponse.playabilityStatus?.status != "LOGIN_REQUIRED")
        ) {
            bestResponse = resolvedResponse
        }
    }

    bestResponse ?: throw IllegalStateException("YouTube did not return a player response")
}
