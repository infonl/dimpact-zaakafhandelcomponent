package net.atos.zac.app.admin.model

import jakarta.annotation.Nullable
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class RestDocumentCreationParameters(
    val enabledGlobally: Boolean?,

    @Nullable
    val enabledForZaaktype: Boolean? = false
)
