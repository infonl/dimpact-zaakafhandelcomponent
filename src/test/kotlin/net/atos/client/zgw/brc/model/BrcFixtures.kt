/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.brc.model

import net.atos.client.zgw.brc.model.generated.Besluit
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
    expirationDate: LocalDate? = LocalDate.now().plusDays(1),
    url: URI = URI("http://localhost/besluit/${UUID.randomUUID()}"),
    vervalredenWeergave: String = "dummyVervalredenWeergave"
) = Besluit(url, vervalredenWeergave).apply {
    this.identificatie = identificatie
    this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie
    this.besluittype = besluittypeUri
    this.zaak = zaakUri
    this.datum = date
    this.toelichting = reason
    this.ingangsdatum = startDate
    this.vervaldatum = expirationDate
}
