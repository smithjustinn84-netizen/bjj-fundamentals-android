package dev.justinsmith.bjj.platform

/** Opens an address outside the app: a YouTube bookmark in the YouTube app or the browser. */
fun interface Links {
    fun open(url: String)
}
