package dev.justinsmith.bjj.di

import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.platform.Links
import dev.justinsmith.bjj.storage.bjjDatabaseBuilder
import dev.justinsmith.bjj.storage.roomStorage
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.TimeZone
import java.util.UUID

/** Starts the app's container over the application's [context]: `BjjApplication`'s, once per process. */
fun initAppKoin(context: Context): KoinApplication = startKoin { modules(appModules(androidModule(context))) }

private fun androidModule(context: Context) = module {
    // The one line that picks the storage engine.
    single {
        CoreEnv(
            storage = roomStorage(bjjDatabaseBuilder(context), starterContent(System.currentTimeMillis())),
            newId = { UUID.randomUUID().toString() },
            now = System::currentTimeMillis,
            // Asked each time, so a trip across time zones is followed without a restart.
            zone = LocalZone { TimeZone.getDefault().getOffset(it).toLong() },
        )
    }
    single {
        Links { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
