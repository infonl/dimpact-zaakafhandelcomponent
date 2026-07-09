/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

data class RestDecisionTypePublication(
    val enabled: Boolean,
    val publicationTerm: String?,
    val publicationTermDays: Int?,
    val responseTerm: String?,
    val responseTermDays: Int?
)
