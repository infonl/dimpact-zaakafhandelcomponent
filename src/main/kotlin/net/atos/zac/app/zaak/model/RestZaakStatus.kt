/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.zrc.model.Status
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestZaakStatus(
    var naam: String,

    var toelichting: String
)

fun toRestZaakStatus(
    status: Status,
    statustype: StatusType
) = RestZaakStatus(
    toelichting = status.statustoelichting,
    naam = statustype.omschrijving
)
