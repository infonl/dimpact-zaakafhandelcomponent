/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import jakarta.validation.constraints.NotBlank

data class RestReferenceTable(
    val id: Long,

    @NotBlank
    val code: String,

    @NotBlank
    val name: String,

    val isSystemReferenceTable: Boolean = false,

    val valuesCount: Int = 0,

    val values: List<RestReferenceTableValue> = emptyList()
)
