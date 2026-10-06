/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model.besluit

import jakarta.json.bind.annotation.JsonbProperty

data class RestBesluitTypePublication(
    @get:JsonbProperty("isEnabled")
    val isEnabled: Boolean,

    val publicationTerm: String?,
    val publicationTermDays: Int?,
    val responseTerm: String?,
    val responseTermDays: Int?
)
