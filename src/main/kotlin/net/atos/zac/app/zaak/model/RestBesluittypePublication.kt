/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak.model

data class RestBesluittypePublication(
    val enabled: Boolean,
    val publicationTerm: String?,
    val publicationTermDays: Int?,
    val responseTerm: String?,
    val responseTermDays: Int?
)
