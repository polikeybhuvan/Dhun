package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.enums.PlaylistSortBy
import it.vfsfitvnm.vimusic.enums.SortOrder
import it.vfsfitvnm.vimusic.models.Playlist
import it.vfsfitvnm.vimusic.models.PlaylistPreview
import it.vfsfitvnm.vimusic.models.SongPlaylistMap
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.HeaderIconButton
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.components.themed.TextFieldDialog
import it.vfsfitvnm.vimusic.ui.items.PlaylistItem
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.playlistSortByKey
import it.vfsfitvnm.vimusic.utils.playlistSortOrderKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.toast

private fun youtubeVideoId(line: String): String? = runCatching {
    val uri = Uri.parse(line.trim())
    if (uri.scheme != "https") return@runCatching null
    val host = uri.host?.lowercase() ?: return@runCatching null
    val candidate = when {
        host == "youtu.be" && uri.pathSegments.size == 1 -> uri.lastPathSegment
        host == "youtube.com" || host == "www.youtube.com" || host == "music.youtube.com" ->
            if (uri.path == "/watch") uri.getQueryParameter("v") else null
        else -> null
    }
    candidate?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}.getOrNull()

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomePlaylists(
    onBuiltInPlaylist: (BuiltInPlaylist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSearchClick: () -> Unit,
) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        query {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read playlist file")
                require(text.length <= 2 * 1024 * 1024) { "Playlist file is too large" }
                val ids = text.lineSequence().mapNotNull(::youtubeVideoId).distinct().take(500).toList()
                val songs = ids.mapNotNull(Database::songOnce)
                if (songs.isEmpty()) error("No tracks from this playlist are in your library")
                val name = text.lineSequence().firstOrNull { it.startsWith("#PLAYLIST:") }
                    ?.removePrefix("#PLAYLIST:")?.trim()?.take(80)?.takeIf(String::isNotBlank)
                    ?: "Imported playlist"
                val id = Database.insert(Playlist(name = name))
                if (id > 0) Database.insertSongPlaylistMaps(
                    songs.mapIndexed { index, song -> SongPlaylistMap(songId = song.id, playlistId = id, position = index) }
                )
                android.os.Handler(android.os.Looper.getMainLooper()).post { context.toast("Imported ${songs.size} tracks") }
            }.onFailure {
                val message = "Playlist import failed: ${it.message ?: "invalid file"}"
                android.os.Handler(android.os.Looper.getMainLooper()).post { context.toast(message) }
            }
        }
    }

    var isCreatingANewPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    if (isCreatingANewPlaylist) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            onDismiss = {
                isCreatingANewPlaylist = false
            },
            onDone = { text ->
                query {
                    Database.insert(Playlist(name = text))
                }
            }
        )
    }

    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Descending)

    var items by persistList<PlaylistPreview>("home/playlists")

    LaunchedEffect(sortBy, sortOrder) {
        Database.playlistPreviews(sortBy, sortOrder).collect { items = it }
    }

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    val thumbnailSizeDp = 108.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    val lazyGridState = rememberLazyGridState()

    Box {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(Dimensions.thumbnails.song * 2 + Dimensions.itemsVerticalPadding * 2),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(Dimensions.itemsVerticalPadding * 2),
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimensions.itemsVerticalPadding * 2,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette.background0)
        ) {
            item(key = "header", contentType = 0, span = { GridItemSpan(maxLineSpan) }) {
                Header(title = "Playlists") {
                    SecondaryTextButton(
                        text = "New playlist",
                        onClick = { isCreatingANewPlaylist = true }
                    )

                    SecondaryTextButton(
                        text = "Import M3U",
                        onClick = {
                            try { importLauncher.launch(arrayOf("audio/x-mpegurl", "application/octet-stream", "text/plain")) }
                            catch (_: Exception) { context.toast("Couldn't open a playlist file") }
                        }
                    )

                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )

                    HeaderIconButton(
                        icon = R.drawable.medical,
                        color = if (sortBy == PlaylistSortBy.SongCount) colorPalette.text else colorPalette.textDisabled,
                        onClick = { sortBy = PlaylistSortBy.SongCount }
                    )

                    HeaderIconButton(
                        icon = R.drawable.text,
                        color = if (sortBy == PlaylistSortBy.Name) colorPalette.text else colorPalette.textDisabled,
                        onClick = { sortBy = PlaylistSortBy.Name }
                    )

                    HeaderIconButton(
                        icon = R.drawable.time,
                        color = if (sortBy == PlaylistSortBy.DateAdded) colorPalette.text else colorPalette.textDisabled,
                        onClick = { sortBy = PlaylistSortBy.DateAdded }
                    )

                    Spacer(
                        modifier = Modifier
                            .width(2.dp)
                    )

                    HeaderIconButton(
                        icon = R.drawable.arrow_up,
                        color = colorPalette.text,
                        onClick = { sortOrder = !sortOrder },
                        modifier = Modifier
                            .graphicsLayer { rotationZ = sortOrderIconRotation }
                    )
                }
            }

            item(key = "favorites") {
                PlaylistItem(
                    icon = R.drawable.heart,
                    colorTint = colorPalette.red,
                    name = "Favorites",
                    songCount = null,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier
                        .clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Favorites) })
                        .animateItemPlacement()
                )
            }

            item(key = "offline") {
                PlaylistItem(
                    icon = R.drawable.airplane,
                    colorTint = colorPalette.blue,
                    name = "Offline",
                    songCount = null,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier
                        .clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Offline) })
                        .animateItemPlacement()
                )
            }

            items(items = items, key = { it.playlist.id }) { playlistPreview ->
                PlaylistItem(
                    playlist = playlistPreview,
                    thumbnailSizeDp = thumbnailSizeDp,
                    thumbnailSizePx = thumbnailSizePx,
                    alternative = true,
                    modifier = Modifier
                        .clickable(onClick = { onPlaylistClick(playlistPreview.playlist) })
                        .animateItemPlacement()
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
