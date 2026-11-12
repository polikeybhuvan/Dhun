package it.vfsfitvnm.vimusic.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import it.vfsfitvnm.vimusic.BuildConfig
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.secondary

@ExperimentalAnimationApi
@Composable
fun About() {
    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current

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
        Header(title = "About") {
            BasicText(
                text = "v${BuildConfig.VERSION_NAME} · Har gaane mein ek kahaani.",
                style = typography.s.secondary
            )
        }

        SettingsEntryGroupText(title = "DHUN")

        SettingsEntry(
            title = "Har gaane mein ek kahaani.",
            text = "An independent, open-source music player maintained by @polikeybhuvan",
            onClick = {
                uriHandler.openUri("https://github.com/polikeybhuvan/Dhun")
            }
        )

        SettingsEntry(
            title = "GitHub",
            text = "View Dhun's source code and releases",
            onClick = {
                uriHandler.openUri("https://github.com/polikeybhuvan/Dhun")
            }
        )

        SettingsEntry(
            title = "Original project",
            text = "Dhun is based on ViMusic and preserves its GPL-3.0 notices",
            onClick = {
                uriHandler.openUri("https://github.com/vfsfitvnm/ViMusic")
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "TROUBLESHOOTING")

        SettingsEntry(
            title = "Report an issue",
            text = "You will be redirected to GitHub",
            onClick = {
                uriHandler.openUri("https://github.com/polikeybhuvan/Dhun/issues/new?template=bug_report.yaml")
            }
        )

        SettingsEntry(
            title = "Request a feature or suggest an idea",
            text = "You will be redirected to GitHub",
            onClick = {
                uriHandler.openUri("https://github.com/polikeybhuvan/Dhun/issues/new?template=feature_request.yaml")
            }
        )
    }
}
