/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestStatustype(
    var id: UUID,
    var naam: String? = null,
    var naamGeneriek: String? = null,
    var statustekst: String? = null,
    var volgnummer: Int? = null,
    var isEindstatus: Boolean? = null,
    var toelichting: String? = null
)

fun StatusType.toRestStatusType() = RestStatustype(
    id = this.url.extractUuid(),
    naam = this.omschrijving,
    naamGeneriek = this.omschrijvingGeneriek,
    statustekst = this.statustekst,
    volgnummer = this.volgnummer,
    isEindstatus = this.isEindstatus,
    toelichting = this.toelichting
)

fun List<StatusType>.toRestResultaatTypes(): List<RestStatustype> = this.map { it.toRestStatusType() }
