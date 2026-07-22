/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import nl.info.zac.app.search.model.RestDatumRange

fun createRestFindLinkableZakenRequest(
    zoekZaakIdentifier: String? = "ZAAK-2000-00002",
    zoekZaakOmschrijving: String? = null,
    zoekZaakTypeOmschrijving: String? = null,
    startdatum: RestDatumRange? = null,
    einddatum: RestDatumRange? = null,
    relationType: RelatieType = RelatieType.GERELATEERD,
    page: Int = 0,
    rows: Int = 10
) = RestFindLinkableZakenRequest().apply {
    this.zoekZaakIdentifier = zoekZaakIdentifier
    this.zoekZaakOmschrijving = zoekZaakOmschrijving
    this.zoekZaakTypeOmschrijving = zoekZaakTypeOmschrijving
    this.startdatum = startdatum
    this.einddatum = einddatum
    this.relationType = relationType
    this.page = page
    this.rows = rows
}
