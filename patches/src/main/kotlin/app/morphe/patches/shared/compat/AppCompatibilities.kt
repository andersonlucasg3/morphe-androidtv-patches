package app.morphe.patches.shared.compat

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object AppCompatibilities {

    val PARAMOUNT_TV = Compatibility(
        name = "Paramount+ Android TV",
        packageName = "com.cbs.ott",
        appIconColor = 0x0064FF,
        targets = listOf(AppTarget("16.8.0")),
    )

    val DISNEY_PLUS_TV = Compatibility(
        name = "Disney+ Android TV",
        packageName = "com.disney.disneyplus",
        appIconColor = 0x113CCF,
    )

    val HBO_TV = Compatibility(
    name = "HBO Max",
    packageName = "com.wbd.hbomax",
    appIconColor = 0xFFFFFF,
    targets = listOf(AppTarget("6.17.1.4")),
    )
}
