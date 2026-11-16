package dev.justinsmith.bjj.bindings

import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LocalZone

/**
 * The time and the time zone, for a ViewModel working out what is due or which day it is. The same clock the
 * core reads, without the rest of `CoreEnv`: a ViewModel has no business with the storage.
 */
class Clock(env: CoreEnv) {
    private val read = env.now
    val zone: LocalZone = env.zone

    /** Epoch milliseconds, now. */
    fun now(): Long = read()
}
