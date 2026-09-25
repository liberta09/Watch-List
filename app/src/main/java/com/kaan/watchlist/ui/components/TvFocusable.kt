package com.kaan.watchlist.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kaan.watchlist.ui.theme.BlueAccent

@Composable
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(8.dp),
    focusBorderWidth: Dp = 2.dp,
    focusBorderColor: Color = BlueAccent,
    scaleOnFocus: Float = 1.04f,
    focusRequester: FocusRequester? = null,
    onClick: (() -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val internalFocusRequester = focusRequester ?: remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1f,
        label = "focusScale"
    )

    var modifier = this
        .focusRequester(internalFocusRequester)
        .onFocusChanged { isFocused = it.isFocused }
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier.border(focusBorderWidth, focusBorderColor, shape)
            } else {
                Modifier
            }
        )

    if (onClick != null) {
        modifier = modifier
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    }

    return modifier
}
