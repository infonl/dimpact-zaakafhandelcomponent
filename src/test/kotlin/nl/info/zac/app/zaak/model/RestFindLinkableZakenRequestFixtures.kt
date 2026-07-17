/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import nl.info.zac.app.klant.model.klant.IdentificatieType
import java.util.UUID

fun createRestFindLinkableZakenRequest(
    zaakUuid: UUID = UUID.randomUUID(),
    zoekZaakIdentifier: String? = "ZAAK-2000-00002",
    zoekZaakOmschrijving: String? = null,
    zoekZaakType: UUID? = null,
    relationType: RelatieType = RelatieType.GERELATEERD,
    page: Int = 0,
    rows: Int = 10
) = RestFindLinkableZakenRequest().apply {
    this.zaakUuid = zaakUuid
    this.zoekZaakIdentifier = zoekZaakIdentifier
    this.zoekZaakOmschrijving = zoekZaakOmschrijving
    this.zoekZaakType = zoekZaakType
    this.relationType = relationType
    this.page = page
    this.rows = rows
}
