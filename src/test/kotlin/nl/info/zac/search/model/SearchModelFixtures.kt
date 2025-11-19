/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search.model

import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

@Suppress("LongParameterList")
fun createZaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zaaktypeIdentificatie: String = "fakeZaaktypeIdentificatie",
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    zaaktypeUuid: String = UUID.randomUUID().toString(),
    identificatie: String = "identificatie",
    omschrijving: String = "fakeOmschrijving",
    statustypeOmschrijving: String = "fakeStatustypeOmschrijving",
    archiefNominatie: String = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
    indicatie: ZaakIndicatie? = null,
    behandelaarGebruikersnaam: String? = null
) = ZaakZoekObject(
    id = uuidAsString,
    type = type.name,
    identificatie = "fakeZaakIdentificatie",
    zaaktypeIdentificatie = zaaktypeIdentificatie,
    zaaktypeUuid = zaaktypeUuid,
    zaaktypeOmschrijving = zaaktypeOmschrijving
).apply {
    this.identificatie = identificatie
    this.omschrijving = omschrijving
    this.statustypeOmschrijving = statustypeOmschrijving
    this.archiefNominatie = archiefNominatie
    this.behandelaarGebruikersnaam = behandelaarGebruikersnaam
    indicatie?.let { setIndicatie(it, true) }
}

@Suppress("LongParameterList")
fun createTaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.TAAK,
    zaaktypeIdentificatie: String = "fakeZaaktypeIdentificatie",
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    zaaktypeUuid: String = UUID.randomUUID().toString(),
    zaakIdentificatie: String = "identificatie",
    zaakOmschrijving: String = "fakeOmschrijving",
    behandelaarGebruikersnaam: String? = null
) = TaakZoekObject(
    id = uuidAsString,
    type = type.name,
    zaakIdentificatie = zaakIdentificatie,
    zaakOmschrijving = zaakOmschrijving,
    zaaktypeIdentificatie = zaaktypeIdentificatie,
    zaaktypeUuid = zaaktypeUuid,
    zaaktypeOmschrijving = zaaktypeOmschrijving
).apply {
    this.zaakIdentificatie = zaakIdentificatie
    this.zaakOmschrijving = zaakOmschrijving
    this.behandelaarGebruikersnaam = behandelaarGebruikersnaam
}
