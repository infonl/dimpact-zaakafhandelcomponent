package net.atos.zac.app.signalering.model

import java.time.ZonedDateTime

fun createRestSignaleringTaskSummary(
    id: String = "id",
    naam: String = "naam",
    zaakIdentificatie: String = "zakIdentificatie",
    zaaktypeOmschrijving: String = "zaaktypeOmschrijving",
    creatiedatumTijd: ZonedDateTime = ZonedDateTime.now(),
) = RestSignaleringTaskSummary(
    id = id,
    naam = naam,
    zaakIdentificatie = zaakIdentificatie,
    zaaktypeOmschrijving = zaaktypeOmschrijving,
    creatiedatumTijd = creatiedatumTijd
)
