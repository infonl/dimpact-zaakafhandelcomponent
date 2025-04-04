/*
 *  SPDX-FileCopyrightText: 2025 Lifely
 *  SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import nl.info.zac.search.model.zoekobject.ZoekObjectType

data class RestZaakKoppelenZoekObject(
    override val id: String? = null,
    override val type: ZoekObjectType? = null,
    override val identificatie: String? = null,
    val omschrijving: String? = null,
    val toelichting: String? = null,
    val zaaktypeOmschrijving: String? = null,
    val statustypeOmschrijving: String? = null,
    val documentKoppelbaar: Boolean = false
) : AbstractRestZoekObject(id, type, identificatie)
