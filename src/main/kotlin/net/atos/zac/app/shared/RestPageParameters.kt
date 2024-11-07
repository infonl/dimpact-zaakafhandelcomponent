package net.atos.zac.app.shared

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
open class RestPageParameters(
    @JvmField val page: Int,
    @JvmField val rows: Int
)
