package com.kaan.watchlist.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kaan.watchlist.ui.theme.BlueAccent

@Composable
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(8.dp),
    focusBorderWidth: Dp = 2.dp,
    focusBorderColor: Color = BlueAccent,
    scaleOnFocus: Float = 1.04f
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1f,
        label = "focusScale"
    )

    return this
        .onFocusChanged { isFocused = it.isFocused }
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier.border(focusBorderWidth, focusBorderColor, shape)
            } else {
                Modifier
            }
        )
}
