package it.vfsfitvnm.vimusic.ui.styling

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Adds a crisp ink outline and a small, offset shadow for a hand-drawn 2D surface feel. */
fun Modifier.cartoonSurface(
    shape: Shape,
    color: Color,
    ink: Color,
    cornerRadius: Dp,
    outlineWidth: Dp,
    shadowOffset: Dp
): Modifier = drawBehind {
    val radius = cornerRadius.toPx()
    drawRoundRect(
        color = ink.copy(alpha = 0.22f),
        topLeft = Offset(x = 0f, y = shadowOffset.toPx()),
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )
}.clip(shape)
    .background(color)
    .border(
        width = outlineWidth,
        color = ink.copy(alpha = 0.34f),
        shape = shape
    )
