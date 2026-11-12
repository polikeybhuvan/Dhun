package it.vfsfitvnm.vimusic.ui.screens.player

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import it.vfsfitvnm.compose.routing.OnGlobalRoute
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.components.BottomSheet
import it.vfsfitvnm.vimusic.ui.components.BottomSheetState
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.rememberBottomSheetState
import it.vfsfitvnm.vimusic.ui.components.themed.BaseMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.ValueSelectorDialog
import it.vfsfitvnm.vimusic.ui.components.themed.IconButton
import it.vfsfitvnm.vimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.cartoonSurface
import it.vfsfitvnm.vimusic.ui.styling.collapsedPlayerProgressBar
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.DisposableListener
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.isLandscape
import it.vfsfitvnm.vimusic.utils.positionAndDurationState
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.shouldBePlaying
import it.vfsfitvnm.vimusic.utils.thumbnail
import kotlin.math.absoluteValue

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun Player(
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current

    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    var shouldBePlaying by remember {
        mutableStateOf(binder.player.shouldBePlaying)
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return

    val positionAndDuration by binder.player.positionAndDurationState()

    val windowInsets = WindowInsets.systemBars

    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    OnGlobalRoute {
        layoutState.collapseSoft()
    }

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        onDismiss = {
            binder.stopRadio()
            binder.player.clearMediaItems()
        },
        collapsedContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .background(colorPalette.background1)
                    .fillMaxSize()
                    .padding(horizontalBottomPaddingValues)
                    .drawBehind {
                        val progress =
                            positionAndDuration.first.toFloat() / positionAndDuration.second.absoluteValue

                        drawLine(
                            color = colorPalette.collapsedPlayerProgressBar,
                            start = Offset(x = 0f, y = 1.dp.toPx()),
                            end = Offset(x = size.width * progress, y = 1.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
            ) {
                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(Dimensions.collapsedPlayer)
                ) {
                    AsyncImage(
                        model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .size(48.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(Dimensions.collapsedPlayer)
                        .weight(1f)
                ) {
                    BasicText(
                        text = mediaItem.mediaMetadata.title?.toString() ?: "",
                        style = typography.xs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                        style = typography.xs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(Dimensions.collapsedPlayer)
                ) {
                    IconButton(
                        icon = if (shouldBePlaying) R.drawable.pause else R.drawable.play,
                        color = colorPalette.text,
                        onClick = {
                            if (shouldBePlaying) {
                                binder.player.pause()
                            } else {
                                if (binder.player.playbackState == Player.STATE_IDLE) {
                                    binder.player.prepare()
                                }
                                binder.player.play()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .size(20.dp)
                    )

                    IconButton(
                        icon = R.drawable.play_skip_forward,
                        color = colorPalette.text,
                        onClick = binder.player::forceSeekToNext,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .size(20.dp)
                    )
                }

                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                )
            }
        }
    ) {
        var isShowingLyrics by rememberSaveable {
            mutableStateOf(false)
        }

        var isShowingStatsForNerds by rememberSaveable {
            mutableStateOf(false)
        }

        val playerBottomSheetState = rememberBottomSheetState(
            64.dp + horizontalBottomPaddingValues.calculateBottomPadding(),
            layoutState.expandedBound
        )

        val containerModifier = Modifier
            .background(colorPalette.background0)
            .padding(
                windowInsets
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .asPaddingValues()
            )
            .padding(bottom = playerBottomSheetState.collapsedBound)

        val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
            Thumbnail(
                isShowingLyrics = isShowingLyrics,
                onShowLyrics = { isShowingLyrics = it },
                isShowingStatsForNerds = isShowingStatsForNerds,
                onShowStatsForNerds = { isShowingStatsForNerds = it },
                modifier = modifier
                    .nestedScroll(layoutState.preUpPostDownNestedScrollConnection)
            )
        }

        val addCurrentSongToPlaylist: () -> Unit = {
            menuState.display {
                NonQueuedMediaItemMenu(
                    onDismiss = menuState::hide,
                    mediaItem = mediaItem,
                    startWithPlaylists = true
                )
            }
        }

        val controlsContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
                Controls(
                    mediaId = mediaItem.mediaId,
                    title = mediaItem.mediaMetadata.title?.toString(),
                    artist = mediaItem.mediaMetadata.artist?.toString(),
                    shouldBePlaying = shouldBePlaying,
                    position = positionAndDuration.first,
                    duration = positionAndDuration.second,
                    onMoreClick = {
                        menuState.display {
                            PlayerMenu(
                                onDismiss = menuState::hide,
                                mediaItem = mediaItem,
                                binder = binder
                            )
                        }
                    },
                    onAddToPlaylist = addCurrentSongToPlaylist,
                    modifier = modifier
                )
        }

        val playerCardShape = RoundedCornerShape(30.dp)
        val playerCardColor = colorPalette.playerCardBackground
        val playerCardInk = if (colorPalette.isDark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color(0xff4a2c17)

        if (isLandscape) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = containerModifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .fillMaxHeight(0.9f)
                        .cartoonSurface(
                            playerCardShape,
                            playerCardColor,
                            playerCardInk,
                            cornerRadius = 30.dp,
                            outlineWidth = 2.5.dp,
                            shadowOffset = 7.dp
                        )
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(0.66f)
                                .padding(bottom = 8.dp)
                        ) {
                            thumbnailContent(Modifier.padding(horizontal = 12.dp))
                        }

                        controlsContent(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxHeight()
                                .weight(1f)
                        )
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = containerModifier.fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .cartoonSurface(
                            playerCardShape,
                            playerCardColor,
                            playerCardInk,
                            cornerRadius = 30.dp,
                            outlineWidth = 2.5.dp,
                            shadowOffset = 7.dp
                        )
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .cartoonSurface(
                                shape = RoundedCornerShape(22.dp),
                                color = if (colorPalette.isDark) androidx.compose.ui.graphics.Color.Black else colorPalette.tileBackground,
                                ink = if (colorPalette.isDark) colorPalette.textSecondary else playerCardInk,
                                cornerRadius = 22.dp,
                                outlineWidth = 2.dp,
                                shadowOffset = 5.dp
                            )
                            .padding(8.dp)
                    ) {
                        thumbnailContent(Modifier.fillMaxSize())
                    }

                    controlsContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(top = 8.dp)
                    )
                }
            }
        }


        Queue(
            layoutState = playerBottomSheetState,
            content = {},
            backgroundColorProvider = { colorPalette.background2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }
}

@ExperimentalAnimationApi
@Composable
private fun PlayerMenu(
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit
) {
    var isShowingPlaybackSpeedDialog by remember { mutableStateOf(false) }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onShowPlaybackSpeed = { isShowingPlaybackSpeedDialog = true },
        onShowSleepTimer = {},
        onDismiss = onDismiss
    )

    if (isShowingPlaybackSpeedDialog) {
        val speeds = listOf(0.5f, 1f, 1.5f, 2f)
        ValueSelectorDialog(
            onDismiss = { isShowingPlaybackSpeedDialog = false },
            title = "Playback speed",
            selectedValue = binder.player.playbackParameters.speed,
            values = speeds,
            onValueSelected = binder.player::setPlaybackSpeed,
            valueText = { "${it}x" }
        )
    }
}
