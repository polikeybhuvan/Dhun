package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.internal
import it.vfsfitvnm.vimusic.path
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.ConfirmationDialog
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.toast
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalAnimationApi
@Composable
fun DatabaseSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    var isConfirmingRestore by remember { mutableStateOf(false) }

    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        FileInputStream(Database.internal.path).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                var temporaryFile: File? = null
                var recoveryFile: File? = null
                var databaseWasClosed = false
                try {
                    val databaseFile = File(requireNotNull(Database.internal.path))
                    val databaseDirectory = requireNotNull(databaseFile.parentFile)
                    temporaryFile = File(databaseDirectory, "restore-${System.currentTimeMillis()}.tmp")
                    val previousFile = File(databaseDirectory, "restore-previous.tmp")
                    recoveryFile = previousFile
                    val candidateFile = requireNotNull(temporaryFile)
                    val input = context.applicationContext.contentResolver.openInputStream(uri)
                        ?: error("Unable to read backup")
                    input.use { source ->
                        FileOutputStream(candidateFile).use { destination ->
                            val buffer = ByteArray(8192)
                            var total = 0L
                            while (true) {
                                val read = source.read(buffer)
                                if (read < 0) break
                                total += read
                                require(total <= 128L * 1024 * 1024) { "Backup is larger than 128 MB" }
                                destination.write(buffer, 0, read)
                            }
                        }
                    }

                    val candidate = android.database.sqlite.SQLiteDatabase.openDatabase(
                        candidateFile.absolutePath,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    )
                    candidate.use { sqlite ->
                        val integrity = sqlite.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else ""
                        }
                        require(integrity == "ok") { "Backup database failed its integrity check" }
                        val requiredTables = listOf("room_master_table", "Song", "Playlist", "SongPlaylistMap")
                        val existingTables = sqlite.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table'",
                            null
                        ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }
                        require(existingTables.containsAll(requiredTables)) { "This file is not a Dhun database backup" }
                    }

                    context.stopService(context.intent<PlayerService>())
                    Database.checkpoint()
                    Database.internal.close()
                    databaseWasClosed = true
                    if (previousFile.exists()) previousFile.delete()
                    require(databaseFile.renameTo(previousFile)) { "Couldn't preserve the current database" }
                    if (!candidateFile.renameTo(databaseFile)) {
                        previousFile.renameTo(databaseFile)
                        error("Couldn't install the restored database")
                    }
                    previousFile.delete()
                    exitProcess(0)
                } catch (error: Throwable) {
                    temporaryFile?.delete()
                    if (databaseWasClosed) {
                        val liveDatabase = File(requireNotNull(Database.internal.path))
                        if (!liveDatabase.exists()) recoveryFile?.renameTo(liveDatabase)
                        exitProcess(0)
                    }
                    val message = "Restore failed: ${error.message ?: "invalid backup"}"
                    android.os.Handler(android.os.Looper.getMainLooper()).post { context.toast(message) }
                }
            }
        }

    if (isConfirmingRestore) {
        ConfirmationDialog(
            text = "Choosing a backup will replace Dhun's current library and listening history. Continue to select a backup file?",
            onDismiss = { isConfirmingRestore = false },
            cancelText = "Cancel",
            confirmText = "Choose backup",
            onConfirm = {
                try {
                    restoreLauncher.launch(
                        arrayOf(
                            "application/vnd.sqlite3",
                            "application/x-sqlite3",
                            "application/octet-stream"
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to open documents")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = "Database")

        SettingsEntryGroupText(title = "CLEANUP")

        SettingsEntry(
            title = "Clear Quick Picks history",
            text = if (eventsCount > 0) {
                "Remove $eventsCount listening events used for Quick Picks"
            } else {
                "Quick Picks history is empty"
            },
            isEnabled = eventsCount > 0,
            onClick = { query(Database::clearEvents) }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "BACKUP")

        SettingsDescription(text = "Personal preferences (i.e. the theme mode) and the cache are excluded.")

        SettingsEntry(
            title = "Backup",
            text = "Export the database to the external storage",
            onClick = {
                @SuppressLint("SimpleDateFormat")
                val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")

                try {
                    backupLauncher.launch("dhun_${dateFormat.format(Date())}.db")
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to create documents")
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "RESTORE")

        ImportantSettingsDescription(
            text = "Restoring replaces Dhun's current songs, playlists, favorites, and listening history with the backup. Downloaded audio files, cache, and preferences are not included. Dhun will close after the restore."
        )

        SettingsEntry(
            title = "Restore",
            text = "Choose a backup to replace current library data",
            onClick = { isConfirmingRestore = true }
        )
    }
}
