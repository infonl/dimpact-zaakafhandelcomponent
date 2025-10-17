/*
 *  SPDX-FileCopyrightText: 2025 INFO.nl
 *  SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.search.model.zoekobject.ZoekObjectType

data class RestZaakKoppelenZoekObject(
    override val id: String? = null,
    override val type: ZoekObjectType? = null,
    override val identificatie: String? = null,
    val omschrijving: String? = null,
    val toelichting: String? = null,
    val zaaktypeOmschrijving: String? = null,
    val statustypeOmschrijving: String? = null,
    @get:JsonbProperty("isKoppelbaar")
    val isKoppelbaar: Boolean = false
) : AbstractRestZoekObject(id, type, identificatie)

fun RestZaakZoekObject.toRestZaakKoppelenZoekObject(documentLinkable: Boolean) =
    RestZaakKoppelenZoekObject(
        id = this.id,
        type = this.type,
        identificatie = this.identificatie,
        omschrijving = this.omschrijving,
        toelichting = this.toelichting,
        zaaktypeOmschrijving = this.zaaktypeOmschrijving,
        statustypeOmschrijving = this.statustypeOmschrijving,
        isKoppelbaar = documentLinkable
    )
