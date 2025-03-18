/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.brc.model

import nl.info.client.zgw.brc.model.generated.Besluit
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createBesluit(
    identificatie: String = "dummyIdentificatie",
    verantwoordelijkeOrganisatie: String = "dummyVerantwoordelijkeOrganisatie",
    besluittypeUri: URI = URI("http://localhost/besluittype/${UUID.randomUUID()}"),
    zaakUri: URI = URI("http://localhost/zaak/${UUID.randomUUID()}"),
    date: LocalDate = LocalDate.now(),
    reason: String = "dummyReason",
    startDate: LocalDate = LocalDate.now(),
    fatalDate: LocalDate = LocalDate.now().plusDays(1),
    publicationDate: LocalDate? = null,
    reactionDate: LocalDate? = null,
    url: URI = URI("http://localhost/besluit/${UUID.randomUUID()}"),
    vervalredenWeergave: String = "dummyVervalredenWeergave"
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
