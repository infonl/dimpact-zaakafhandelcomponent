/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model

data class Sorting(
    val field: String? = null,
    val direction: SorteerRichting? = null
)
