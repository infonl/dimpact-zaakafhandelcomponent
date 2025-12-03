/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.zrc.model.generated.Status
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestZaakStatus(
    val naam: String,
    val toelichting: String
)

fun toRestZaakStatus(
    statustype: StatusType,
    status: Status
) = RestZaakStatus(
    naam = statustype.omschrijving,
    toelichting = status.statustoelichting
)
