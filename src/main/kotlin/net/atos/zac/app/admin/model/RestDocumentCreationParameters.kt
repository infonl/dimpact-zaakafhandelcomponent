package net.atos.zac.app.admin.model

data class RestDocumentCreationParameters(
    val enabledGlobally: Boolean = false,
    val enabledForZaaktype: Boolean = false,
)
