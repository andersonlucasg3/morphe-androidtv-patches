package ajstrick81.morphe.patches.youtubetv.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "YouTube Android TV",
        packageName = "com.google.android.youtube.tv",
        appIconColor = 0xFF0000,
        targets = listOf(AppTarget("7.05.301"))
    )
}
