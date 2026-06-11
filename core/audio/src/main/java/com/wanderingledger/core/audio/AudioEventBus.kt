package com.wanderingledger.core.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight event bus for audio events. Callers emit events; [AudioManager] collects and plays
 * them. Using a SharedFlow with replay=0 means events are fire-and-forget — no backpressure, no
 * queuing of missed events.
 */
class AudioEventBus {
    private val _events = MutableSharedFlow<AudioEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AudioEvent> = _events.asSharedFlow()

    /** Emits an event. Returns false if the buffer is full (event is dropped). */
    fun emit(event: AudioEvent): Boolean = _events.tryEmit(event)
}
