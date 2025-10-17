/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.signalering.model

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
