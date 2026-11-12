package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isIgnoringBatteryOptimizations
import it.vfsfitvnm.vimusic.utils.pauseSearchHistoryKey
import it.vfsfitvnm.vimusic.utils.persistentQueueKey
import it.vfsfitvnm.vimusic.utils.resumePlaybackWhenDeviceConnectedKey
import it.vfsfitvnm.vimusic.utils.skipSilenceKey
import it.vfsfitvnm.vimusic.utils.volumeNormalizationKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)
    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(resumePlaybackWhenDeviceConnectedKey, false)
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

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
        Header(title = "Other")

        SettingsEntryGroupText(title = "PLAYER")

        SwitchSettingEntry(
            title = "Persistent queue",
            text = "Save and restore playing songs",
            isChecked = persistentQueue,
            onCheckedChange = { persistentQueue = it }
        )

        if (isAtLeastAndroid6) {
            SwitchSettingEntry(
                title = "Resume playback",
                text = "When a wired or bluetooth device is connected",
                isChecked = resumePlaybackWhenDeviceConnected,
                onCheckedChange = { resumePlaybackWhenDeviceConnected = it }
            )
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "AUDIO")

        SwitchSettingEntry(
            title = "Skip silence",
            text = "Skip silent parts during playback",
            isChecked = skipSilence,
            onCheckedChange = { skipSilence = it }
        )

        SwitchSettingEntry(
            title = "Loudness normalization",
            text = "Adjust the volume to a fixed level",
            isChecked = volumeNormalization,
            onCheckedChange = { volumeNormalization = it }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "SEARCH HISTORY")

        SwitchSettingEntry(
            title = "Pause search history",
            text = "Neither save new searched queries nor show history",
            isChecked = pauseSearchHistory,
            onCheckedChange = { pauseSearchHistory = it }
        )

        SettingsEntry(
            title = "Clear search history",
            text = if (queriesCount > 0) {
                "Delete $queriesCount search queries"
            } else {
                "History is empty"
            },
            isEnabled = queriesCount > 0,
            onClick = { query(Database::clearQueries) }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "SERVICE LIFETIME")

        SettingsDescription(text = "Allow Dhun to run without battery restrictions to help Android keep playback available in the background.")

        SettingsEntry(
            title = "Background playback",
            isEnabled = !isIgnoringBatteryOptimizations,
            text = if (isIgnoringBatteryOptimizations) {
                "Battery use is unrestricted"
            } else {
                "Allow Dhun to run without battery restrictions"
            },
            onClick = {
                if (!isAtLeastAndroid6) return@SettingsEntry

                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find battery optimization settings, please allow Dhun to run in the background")
                    }
                }
            }
        )

    }
}
