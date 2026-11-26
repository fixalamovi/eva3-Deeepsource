package com.example.deepsource.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

// La entrada se repite solo al cambiar de pantalla, no al escribir o actualizar el reloj.
@Composable
internal fun ScreenEntrance(
    route: String,
    durationMillis: Int = 220,
    offsetDivisor: Int = 32,
    content: @Composable () -> Unit
) {
    key(route) {
        val visible = remember {
            MutableTransitionState(false).apply { targetState = true }
        }
        AnimatedVisibility(
            visibleState = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(durationMillis)) + slideInVertically(tween(durationMillis)) { it / offsetDivisor },
            exit = fadeOut(tween(120)),
            label = "Entrada de pantalla"
        ) {
            content()
        }
    }
}
