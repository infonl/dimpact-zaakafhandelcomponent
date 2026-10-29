/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.brc.model

import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createBesluit(
    identificatie: String = "fakeIdentificatie",
    verantwoordelijkeOrganisatie: String = "fakeVerantwoordelijkeOrganisatie",
    besluittypeUri: URI = URI("http://localhost/besluittype/${UUID.randomUUID()}"),
    zaakUri: URI = URI("http://localhost/zaak/${UUID.randomUUID()}"),
    date: LocalDate = LocalDate.now(),
    reason: String = "fakeReason",
    startDate: LocalDate = LocalDate.now(),
    fatalDate: LocalDate = LocalDate.now().plusDays(1),
    publicationDate: LocalDate? = null,
    reactionDate: LocalDate? = null,
    url: URI = URI("http://localhost/besluit/${UUID.randomUUID()}"),
    vervalredenWeergave: String = "fakeVervalredenWeergave"
) = Besluit(url, vervalredenWeergave).apply {
    this.identificatie = identificatie
    this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie
    this.besluittype = besluittypeUri
    this.zaak = zaakUri
    this.datum = date
    this.toelichting = reason
    ingangsdatum = startDate
    vervaldatum = fatalDate
    publicatiedatum = publicationDate
    uiterlijkeReactiedatum = reactionDate
}

@Suppress("LongParameterList")
fun createNillableDatesBesluitPatch(
    toelichting: String? = "fakeToelichting",
    ingangsdatum: LocalDate? = LocalDate.of(2023, 9, 14),
    vervaldatum: LocalDate? = LocalDate.of(2023, 11, 14),
    vervalreden: VervalredenEnum = VervalredenEnum.TIJDELIJK,
    publicatiedatum: LocalDate? = null,
    uiterlijkeReactiedatum: LocalDate? = null
) = NillableDatesBesluitPatch(
    toelichting = toelichting,
    ingangsdatum = ingangsdatum,
    vervaldatum = vervaldatum,
    vervalreden = vervalreden,
    publicatiedatum = publicatiedatum,
    uiterlijkeReactiedatum = uiterlijkeReactiedatum
)
