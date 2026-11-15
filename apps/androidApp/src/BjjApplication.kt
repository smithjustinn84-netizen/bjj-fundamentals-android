package dev.justinsmith.bjj

import android.app.Application
import dev.justinsmith.bjj.di.initAppKoin

/**
 * Starts the app's container once per process, before any Activity runs, so the database is opened once and
 * outlives each Activity.
 */
class BjjApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAppKoin(this)
    }
}
