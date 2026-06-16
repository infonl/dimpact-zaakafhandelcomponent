/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model.besluit

data class RestBesluitTypePublication(
    val enabled: Boolean,
    val publicationTerm: String?,
    val publicationTermDays: Int?,
    val responseTerm: String?,
    val responseTermDays: Int?
)
