/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search.model

import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

@Suppress("LongParameterList")
fun createZaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zaaktypeOmschrijving: String = "fakeZaakTypeOmschrijving",
    identificatie: String = "identificatie",
    omschrijving: String = "fakeOmschrijving",
    statustypeOmschrijving: String = "fakeStatustypeOmschrijving",
    zaaktypeUuid: String = UUID.randomUUID().toString(),
    archiefNominatie: String = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
    indicatie: ZaakIndicatie? = null,
    behandelaarGebruikersnaam: String? = null
) = ZaakZoekObject(
    id = uuidAsString,
    type = type.name,
    identificatie = "fakeZaakIdentificatie"
).apply {
    this.zaaktypeOmschrijving = zaaktypeOmschrijving
    this.identificatie = identificatie
    this.omschrijving = omschrijving
    this.statustypeOmschrijving = statustypeOmschrijving
    this.zaaktypeUuid = zaaktypeUuid
    this.archiefNominatie = archiefNominatie
    this.behandelaarGebruikersnaam = behandelaarGebruikersnaam
    indicatie?.let { setIndicatie(it, true) }
}
