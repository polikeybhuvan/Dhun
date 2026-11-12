package it.vfsfitvnm.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class BrowseResponse(
    val contents: Contents?,
    val header: Header?,
    val microformat: Microformat?,
    val background: Background?
) {
    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs?,
        val sectionListRenderer: SectionListRenderer?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
    )

    @Serializable
    data class TwoColumnBrowseResultsRenderer(
        val tabs: List<Tabs.Tab>?,
        val secondaryContents: SecondaryContents?
    ) {
        @Serializable
        data class SecondaryContents(val sectionListRenderer: SectionListRenderer?)
    }

    @Serializable
    data class Background(val musicThumbnailRenderer: ThumbnailRenderer?)

    @Serializable
    data class Header @OptIn(ExperimentalSerializationApi::class) constructor(
        @JsonNames("musicVisualHeaderRenderer")
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
    ) {
        @Serializable
        data class MusicDetailHeaderRenderer(
            val title: Runs?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?,
        )

        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val description: Runs?,
            val playButton: PlayButton?,
            val startRadioButton: StartRadioButton?,
            val thumbnail: ThumbnailRenderer?,
            val foregroundThumbnail: ThumbnailRenderer?,
            val title: Runs?
        ) {
            @Serializable
            data class PlayButton(
                val buttonRenderer: ButtonRenderer?
            )

            @Serializable
            data class StartRadioButton(
                val buttonRenderer: ButtonRenderer?
            )
        }
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer?
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String?,
            val title: String?,
            val thumbnail: ThumbnailContainer?
        ) {
            @Serializable
            data class ThumbnailContainer(
                val thumbnails: List<it.vfsfitvnm.innertube.models.Thumbnail>?
            )
        }
    }
}
