/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search.model

import net.atos.client.zgw.shared.model.Archiefnominatie
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

fun createZaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zaaktypeOmschrijving: String = "fakeOmschrijving"
) = ZaakZoekObject(
    id = uuidAsString,
    type = type.name
).apply {
    this.zaaktypeOmschrijving = zaaktypeOmschrijving
}

@Suppress("LongParameterList")
fun createZaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zaaktypeOmschrijving: String = "fakeZaakTypeOmschrijving",
    identificatie: String = "identificatie",
    omschrijving: String = "fakeOmschrijving",
    statustypeOmschrijving: String = "fakeStatustypeOmschrijving",
    zaaktypeUuid: String = UUID.randomUUID().toString(),
    archiefNominatie: String = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
    indicatie: ZaakIndicatie? = null,
) = ZaakZoekObject(
    id = uuidAsString,
    type = type.name
).apply {
    this.zaaktypeOmschrijving = zaaktypeOmschrijving
    this.identificatie = identificatie
    this.omschrijving = omschrijving
    this.statustypeOmschrijving = statustypeOmschrijving
    this.zaaktypeUuid = zaaktypeUuid
    this.archiefNominatie = archiefNominatie
    indicatie?.let { setIndicatie(it, true) }
}
