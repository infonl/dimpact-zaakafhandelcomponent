package net.atos.zac.app.signalering.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@AllOpen
@NoArgConstructor
data class RESTSignaleringTaakSummary(
    var id: String,
    var naam: String,
    var zaakIdentificatie: String,
    var zaaktypeOmschrijving: String,
    var creatiedatumTijd: ZonedDateTime
)
