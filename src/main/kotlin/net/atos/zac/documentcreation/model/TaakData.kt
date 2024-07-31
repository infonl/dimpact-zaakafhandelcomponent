/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

data class TaakData(
    val naam: String,
    var behandelaar: String? = null,
    val data: Map<String, String>
)
