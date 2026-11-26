package com.example.deepsource.domain

import java.util.Locale

// Se redondea una sola vez al terminar, después de sumar todos los tramos activos.
fun roundedTimerSeconds(elapsedMs: Long): Long {
    require(elapsedMs > 0) { "Deja transcurrir algo de tiempo antes de guardar" }
    val minutes = elapsedMs / 60_000 + if (elapsedMs % 60_000 == 0L) 0 else 1
    require(minutes <= 1440) { "El registro del cronómetro no puede superar 24 horas" }
    return minutes * 60
}

fun minuteClockText(elapsedMs: Long, roundUp: Boolean = false): String {
    val safe = elapsedMs.coerceAtLeast(0)
    val minutes = safe / 60_000 + if (roundUp && safe % 60_000 != 0L) 1 else 0
    return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
}
