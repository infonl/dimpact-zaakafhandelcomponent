package net.atos.zac.app.shared

import jakarta.validation.constraints.Positive
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
open class RestPageParameters(
    @JvmField @Positive var page: Int,
    @JvmField @Positive var rows: Int
)
