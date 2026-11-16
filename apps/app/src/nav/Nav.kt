package dev.justinsmith.bjj.nav

import androidx.navigation3.runtime.NavKey

/** How a page moves to another: what the app's one back stack offers a screen. */
class Nav(
    /** Opens a page over this one. */
    val open: (NavKey) -> Unit,
    /** Leaves this page for the one under it. */
    val back: () -> Unit,
    /** Shows one of the bar's five destinations, or another page of it. */
    val selectTab: (Tab) -> Unit,
    /** Puts a page where this one is, so Back skips this one. */
    val replace: (NavKey) -> Unit,
)
