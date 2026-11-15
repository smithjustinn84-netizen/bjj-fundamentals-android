package dev.justinsmith.bjj.storage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** The app's database, in the place Android keeps an app's databases. */
fun bjjDatabaseBuilder(context: Context): RoomDatabase.Builder<BjjDatabase> =
    Room.databaseBuilder<BjjDatabase>(context, context.getDatabasePath(DATABASE_NAME).absolutePath)
