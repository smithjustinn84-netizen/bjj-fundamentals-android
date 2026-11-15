package dev.justinsmith.bjj.core.testing

import dev.justinsmith.bjj.core.AppStorage
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.MemoryStorage
import dev.justinsmith.bjj.core.seed.starterContent

/** A core for a test, with what it runs on: its storage and the clock the test moves. */
class TestCore(val core: Core, val storage: AppStorage, val clock: TestClock)

/**
 * A core running on in-memory storage with the starter content, predictable ids ("id-1", "id-2", ...), UTC as
 * its time zone and a clock the test controls. [wrap] lets a test stand something in front of the storage, to
 * make a write fail.
 */
fun testCore(zone: LocalZone = LocalZone.Utc, wrap: (AppStorage) -> AppStorage = { it }): TestCore {
    val clock = TestClock()
    var lastId = 0
    val storage = MemoryStorage(starterContent(TEST_NOW))
    val env = CoreEnv(wrap(storage), newId = { "id-${++lastId}" }, now = { clock.now }, zone = zone)
    return TestCore(Core(env), storage, clock)
}
