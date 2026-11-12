package it.vfsfitvnm.vimusic.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.cartoonSurface
import androidx.compose.ui.draw.clip

@Composable
inline fun ItemContainer(
    alternative: Boolean,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    tile: Boolean = false,
    content: @Composable (centeredModifier: Modifier) -> Unit
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val tileColor = colorPalette.tileBackground
    val tileShape = RoundedCornerShape(18.dp)
    val inkColor = if (colorPalette.isDark) Color.Black else Color(0xff4a2c17)
    if (alternative) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .padding(vertical = if (tile) 6.dp else Dimensions.itemsVerticalPadding, horizontal = if (tile) 8.dp else 16.dp)
                .width(thumbnailSizeDp + if (tile) 24.dp else 0.dp)
                .then(
                    if (tile) Modifier
                        .cartoonSurface(tileShape, tileColor, inkColor, 18.dp, 1.5.dp, 4.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                    else Modifier
                )
        ) {
            content(
                centeredModifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }
    } else {
        Row(
            verticalAlignment = verticalAlignment,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .padding(vertical = if (tile) 6.dp else Dimensions.itemsVerticalPadding, horizontal = 16.dp)
                .fillMaxWidth()
                .then(
                    if (tile) Modifier
                        .cartoonSurface(tileShape, tileColor, inkColor, 18.dp, 1.5.dp, 4.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                    else Modifier
                )
        ) {
            content(
                centeredModifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
inline fun ItemInfoContainer(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
        content = content
    )
}
