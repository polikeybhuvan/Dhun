package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.innertube.models.bodies.NextBody
import it.vfsfitvnm.innertube.requests.relatedPage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.InHistoryMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.items.ArtistItem
import it.vfsfitvnm.vimusic.ui.items.ArtistItemPlaceholder
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.items.SongItemPlaceholder
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun QuickPicks(
    onArtistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    var trending by persist<Song?>("home/trending")

    var relatedPageResult by persist<Result<Innertube.RelatedPage?>?>(tag = "home/relatedPageResult")
    var recentSongs by persistList<Song>("home/quickPicks/recentSongs")
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        Database.recentlyPlayed().distinctUntilChanged().collect { recentSongs = it }
    }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            isRefreshing = true
            val result = withContext(Dispatchers.IO) {
                Innertube.relatedPage(NextBody(videoId = trending?.id ?: "J7p4bzqLvCw"))
            }
            relatedPageResult = result
            recentSongs = Database.recentlyPlayed().first()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        Database.trending().distinctUntilChanged().collect { song ->
            if (relatedPageResult == null || trending?.id != song?.id) {
                relatedPageResult =
                    Innertube.relatedPage(NextBody(videoId = (song?.id ?: "J7p4bzqLvCw")))
            }
            trending = song
        }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px

    val scrollState = rememberScrollState()
    val quickPicksLazyGridState = rememberLazyGridState()
    val recentSongsLazyGridState = rememberLazyGridState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val refreshThreshold = with(density) { 72.dp.toPx() }
    val pullDistance = remember { mutableStateOf(0f) }
    val refreshRotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pullRefreshConnection = remember(scrollState, refreshThreshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && pullDistance.value > 0f && available.y < 0f) {
                    val consumed = available.y.coerceAtLeast(-pullDistance.value)
                    pullDistance.value += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag && scrollState.value == 0 && available.y > 0f) {
                    pullDistance.value = (pullDistance.value + available.y).coerceAtMost(refreshThreshold)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance.value >= refreshThreshold && !isRefreshing) refreshKey++
                pullDistance.value = 0f
                return Velocity.Zero
            }
        }
    }

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    BoxWithConstraints {
        val quickPicksFlingBehavior = rememberColumnSnapFlingBehavior(quickPicksLazyGridState, rows = 4)
        val recentFlingBehavior = rememberColumnSnapFlingBehavior(recentSongsLazyGridState, rows = 4)

        val itemInHorizontalGridWidth = maxWidth

        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .nestedScroll(pullRefreshConnection)
                .verticalScroll(scrollState)
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )
        ) {
            Header(
                title = "Quick picks",
                modifier = Modifier
                    .padding(endPaddingValues)
            )

            if (isRefreshing || pullDistance.value > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = "Refreshing",
                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = if (isRefreshing) refreshRotation else {
                                    (pullDistance.value / refreshThreshold * 360f).coerceIn(0f, 360f)
                                }
                            }
                    )
                    BasicText(
                        text = if (isRefreshing) "Refreshing…" else "Release to refresh",
                        style = typography.xs.secondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            relatedPageResult?.getOrNull()?.let { related ->
                LazyHorizontalGrid(
                    state = quickPicksLazyGridState,
                    rows = GridCells.Fixed(4),
                    flingBehavior = quickPicksFlingBehavior,
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 4)
                ) {
                    trending?.let { song ->
                        item {
                            SongItem(
                                song = song,
                                thumbnailSizePx = songThumbnailSizePx,
                                thumbnailSizeDp = songThumbnailSizeDp,
                                showDuration = false,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    onRemoveFromQuickPicks = {
                                                        query {
                                                            Database.clearEventsFor(song.id)
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            )
                                        }
                                    )
                                    .animateItemPlacement()
                                    .width(itemInHorizontalGridWidth)
                            )
                        }
                    }

                    items(
                        items = related.songs?.dropLast(if (trending == null) 0 else 1)
                            ?: emptyList(),
                        key = Innertube.SongItem::key
                    ) { song ->
                        SongItem(
                            song = song,
                            thumbnailSizePx = songThumbnailSizePx,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            showDuration = false,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem
                                            )
                                        }
                                    },
                                    onClick = {
                                        val mediaItem = song.asMediaItem
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    }
                                )
                                .animateItemPlacement()
                                .width(itemInHorizontalGridWidth)
                        )
                    }
                }

                val quickPickPageCount = (
                    maxOf(related.songs?.size ?: 0, if (trending != null) 1 else 0) + 3
                ) / 4
                if (quickPickPageCount > 1) {
                    ColumnPageDots(
                        pageCount = quickPickPageCount,
                        currentPage = quickPicksLazyGridState.currentColumn(rows = 4),
                        color = colorPalette.searchIcon,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                related.artists?.let { artists ->
                    BasicText(
                        text = "Similar artists",
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = artists,
                            key = Innertube.ArtistItem::key,
                        ) { artist ->
                            ArtistItem(
                                artist = artist,
                                thumbnailSizePx = artistThumbnailSizePx,
                                thumbnailSizeDp = artistThumbnailSizeDp,
                                alternative = true,
                                modifier = Modifier
                                    .clickable(onClick = { onArtistClick(artist.key) })
                            )
                        }
                    }
                }

                if (recentSongs.isNotEmpty()) {
                    BasicText(
                        text = "Recently played",
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyHorizontalGrid(
                        state = recentSongsLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = recentFlingBehavior,
                        contentPadding = endPaddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 4)
                    ) {
                        itemsIndexed(
                            items = recentSongs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            SongItem(
                                song = song,
                                thumbnailSizePx = songThumbnailSizePx,
                                thumbnailSizeDp = songThumbnailSizeDp,
                                showDuration = false,
                                authorsOverride = song.artistsText?.takeUnless {
                                    it.contains("plays", ignoreCase = true) ||
                                        it.contains("views", ignoreCase = true)
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                InHistoryMediaItemMenu(
                                                    song = song,
                                                    onDismiss = menuState::hide
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(
                                                recentSongs.map(Song::asMediaItem),
                                                index
                                            )
                                        }
                                    )
                                    .width(itemInHorizontalGridWidth)
                            )
                        }
                    }

                    val recentPageCount = (recentSongs.size + 3) / 4
                    if (recentPageCount > 1) {
                        ColumnPageDots(
                            pageCount = recentPageCount,
                            currentPage = recentSongsLazyGridState.currentColumn(rows = 4),
                            color = colorPalette.searchIcon,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Unit
            } ?: relatedPageResult?.exceptionOrNull()?.let {
                BasicText(
                    text = "Quick picks are unavailable right now. Try again later.",
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(all = 16.dp)
                )
            } ?: if (relatedPageResult == null) ShimmerHost {
                repeat(4) {
                    SongItemPlaceholder(
                        thumbnailSizeDp = songThumbnailSizeDp,
                    )
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        ArtistItemPlaceholder(
                            thumbnailSizeDp = artistThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }

            } else {
                BasicText(
                    text = "No recommendations available for this song.",
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(all = 16.dp)
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            scrollState = scrollState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )
    }
}

@Composable
private fun ColumnPageDots(
    pageCount: Int,
    currentPage: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        repeat(pageCount) { page ->
            val isCurrentPage = page == currentPage
            val dotSize by animateDpAsState(
                targetValue = if (isCurrentPage) 8.dp else 5.dp,
                animationSpec = tween(durationMillis = 90)
            )
            val dotAlpha by animateFloatAsState(
                targetValue = if (isCurrentPage) 1f else 0.35f,
                animationSpec = tween(durationMillis = 90)
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = dotAlpha))
            )
        }
    }
}

private fun LazyGridState.currentColumn(rows: Int): Int {
    val info = layoutInfo
    val viewportCenter = (info.beforeContentPadding + info.viewportSize.width - info.afterContentPadding) / 2f
    val centeredItem = info.visibleItemsInfo.minByOrNull { item ->
        abs(item.offset.x + item.size.width / 2f - viewportCenter)
    }
    return (centeredItem?.index ?: firstVisibleItemIndex).div(rows)
}

@Composable
private fun rememberColumnSnapFlingBehavior(
    state: LazyGridState,
    rows: Int
): FlingBehavior = remember(state, rows) {
    object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            val info = state.layoutInfo
            val visibleItems = info.visibleItemsInfo
            val anchor = visibleItems.minByOrNull { item ->
                val center = (info.beforeContentPadding + info.viewportSize.width - info.afterContentPadding) / 2f
                abs(item.offset.x + item.size.width / 2f - center)
            } ?: return initialVelocity

            val pageWidth = anchor.size.width.toFloat().coerceAtLeast(1f)
            val anchorPage = anchor.index / rows
            val viewportCenter = (info.beforeContentPadding + info.viewportSize.width - info.afterContentPadding) / 2f
            val anchorCenterOffset = anchor.offset.x + anchor.size.width / 2f - viewportCenter
            val scrollPage = anchorPage - anchorCenterOffset / pageWidth
            val pageCount = (info.totalItemsCount + rows - 1) / rows
            val lastPage = (pageCount - 1).coerceAtLeast(0)
            val targetPage = when {
                initialVelocity > 800f -> kotlin.math.ceil(scrollPage).toInt()
                initialVelocity < -800f -> kotlin.math.floor(scrollPage).toInt()
                else -> kotlin.math.round(scrollPage).toInt()
            }.coerceIn(0, lastPage)
            val targetOffset = anchorCenterOffset + (targetPage - anchorPage) * pageWidth

            if (abs(targetOffset) < 1f) return 0f

            var previousValue = 0f
            Animatable(0f).animateTo(
                targetValue = targetOffset,
                animationSpec = tween(durationMillis = 145, easing = FastOutSlowInEasing)
            ) {
                val requestedScroll = value - previousValue
                val consumedScroll = scrollBy(requestedScroll)
                previousValue += consumedScroll
            }

            return 0f
        }
    }
}



