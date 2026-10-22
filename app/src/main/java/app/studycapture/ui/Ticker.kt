package app.studycapture.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits once a minute so "due" counts follow the clock while a screen is open. */
fun minuteTicker(): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(60_000)
    }
}
