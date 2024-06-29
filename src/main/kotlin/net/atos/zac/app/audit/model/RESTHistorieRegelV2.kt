package net.atos.zac.app.audit.model

import java.time.ZonedDateTime

data class RESTHistorieRegelV2(
    val gegeven: String,
    val actie: Actie,
    val oudeWaarde: String?,
    val nieuweWaarde: String?,
    val datumTijd: ZonedDateTime,
    val door: String?,
    val reden: String?,
)
