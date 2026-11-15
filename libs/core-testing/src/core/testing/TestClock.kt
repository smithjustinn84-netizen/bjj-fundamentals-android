package dev.justinsmith.bjj.core.testing

/** A clock a test moves by hand: `now` is whatever the test last set. */
class TestClock(var now: Long = TEST_NOW) {
    fun advance(millis: Long) {
        now += millis
    }
}

/** Noon on 14 November 2026, UTC: far from midnight, so a test that adds hours stays on the day it expects. */
const val TEST_NOW: Long = 1_794_657_600_000
