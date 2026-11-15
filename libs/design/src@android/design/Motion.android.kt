package dev.justinsmith.bjj.design

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** What the animator scale is when the setting cannot be read: animations on. */
private const val DEFAULT_ANIMATOR_SCALE = 1f

/** "Remove animations" in the system's accessibility settings sets the animator scale to nothing. */
@Composable
actual fun reducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, DEFAULT_ANIMATOR_SCALE) == 0f
    }
}
