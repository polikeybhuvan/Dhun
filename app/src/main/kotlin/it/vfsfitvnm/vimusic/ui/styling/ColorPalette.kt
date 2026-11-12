package it.vfsfitvnm.vimusic.ui.styling

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode

@Immutable
data class ColorPalette(
    val background0: Color,
    val background1: Color,
    val background2: Color,
    val accent: Color,
    val onAccent: Color,
    val red: Color = Color(0xffbf4040),
    val blue: Color = Color(0xff4472cf),
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val isDark: Boolean,
    val tileBackground: Color = background1,
    val navigationRailBackground: Color = background0,
    val playerCardBackground: Color = background2,
    val searchIcon: Color = accent
) {
    companion object : Saver<ColorPalette, List<Any>> {
        override fun restore(value: List<Any>): ColorPalette {
            val isDark = when (value.firstOrNull() as? Int) {
                0, 2 -> true
                1 -> false
                else -> value.getOrNull(1) as? Boolean ?: false
            }

            return if (isDark) DefaultDarkColorPalette else DefaultLightColorPalette
        }

        override fun SaverScope.save(value: ColorPalette) =
            listOf(if (value.isDark) 0 else 1, value.isDark)
    }
}

val DefaultDarkColorPalette = ColorPalette(
    background0 = Color(0xff0c0c0d),
    background1 = Color(0xff2a1d16),
    background2 = Color(0xff3a2a20),
    text = Color(0xfff4e7d8),
    textSecondary = Color(0xffc8af97),
    textDisabled = Color(0xff8c7663),
    accent = Color(0xffd98236),
    onAccent = Color(0xff29170a),
    isDark = true,
    tileBackground = Color(0xff665246),
    navigationRailBackground = Color(0xff241811),
    playerCardBackground = Color(0xffc87531),
    searchIcon = Color(0xffd98236)
)

val DefaultLightColorPalette = ColorPalette(
    background0 = Color(0xfffbf7f0),
    background1 = Color(0xffd8c0a4),
    background2 = Color(0xffdfc8ac),
    text = Color(0xff33251c),
    textSecondary = Color(0xff705a48),
    textDisabled = Color(0xff89715c),
    accent = Color(0xff805333),
    onAccent = Color(0xfffff8ef),
    isDark = false,
    tileBackground = Color(0xfff3ebe0),
    navigationRailBackground = Color(0xffd8c0a4),
    playerCardBackground = Color(0xffe88a35),
    searchIcon = Color(0xffe88a35)
)

val PureBlackColorPalette = DefaultDarkColorPalette.copy(
    background0 = Color.Black,
    background1 = Color.Black,
    background2 = Color.Black
)

fun colorPaletteOf(
    colorPaletteMode: ColorPaletteMode,
    isSystemInDarkMode: Boolean
): ColorPalette = when (colorPaletteMode) {
    ColorPaletteMode.Light -> DefaultLightColorPalette
    ColorPaletteMode.Dark -> DefaultDarkColorPalette
    ColorPaletteMode.System -> if (isSystemInDarkMode) DefaultDarkColorPalette else DefaultLightColorPalette
}

inline val ColorPalette.collapsedPlayerProgressBar: Color
    get() = text

inline val ColorPalette.favoritesIcon: Color
    get() = if (isDark) Color(0xffc29a75) else Color(0xff805333)

inline val ColorPalette.shimmer: Color
    get() = Color(0xff838383)

inline val ColorPalette.primaryButton: Color
    get() = if (this === PureBlackColorPalette) {
        Color(0xFF272727)
    } else {
        background2
    }

inline val ColorPalette.overlay: Color
    get() = PureBlackColorPalette.background0.copy(alpha = 0.75f)

inline val ColorPalette.onOverlay: Color
    get() = PureBlackColorPalette.text

inline val ColorPalette.onOverlayShimmer: Color
    get() = PureBlackColorPalette.shimmer
