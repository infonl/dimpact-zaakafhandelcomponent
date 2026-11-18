/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.signalering.model

import nl.info.zac.time.ZacTime
import java.time.ZonedDateTime

fun createRestSignaleringTaskSummary(
    id: String = "id",
    naam: String = "naam",
    zaakIdentificatie: String = "zakIdentificatie",
    zaaktypeOmschrijving: String = "zaaktypeOmschrijving",
    creatiedatumTijd: ZonedDateTime = ZacTime.now(),
) = RestSignaleringTaskSummary(
    id = id,
    naam = naam,
    zaakIdentificatie = zaakIdentificatie,
    isZaakspecifiekGeautoriseerd = false,
    zaaktypeOmschrijving = zaaktypeOmschrijving,
    creatiedatumTijd = creatiedatumTijd
)
