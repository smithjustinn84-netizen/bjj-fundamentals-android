package dev.justinsmith.bjj.storage

import androidx.room.Room
import androidx.room.RoomDatabase

/** A database in the file at [path]: the JVM only runs tests, which keep theirs in a temporary folder. */
fun bjjDatabaseBuilder(path: String): RoomDatabase.Builder<BjjDatabase> = Room.databaseBuilder<BjjDatabase>(name = path)
