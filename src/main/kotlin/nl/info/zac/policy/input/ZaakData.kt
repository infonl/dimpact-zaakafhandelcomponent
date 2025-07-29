/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class ZaakData(
    @field:JsonbProperty("open")
    val open: Boolean,

    @field:JsonbProperty("zaaktype")
    val zaaktype: String?,

    @field:JsonbProperty("opgeschort")
    val opgeschort: Boolean,

    @field:JsonbProperty("verlengd")
    val verlengd: Boolean,

    @field:JsonbProperty("intake")
    val intake: Boolean?,

    @field:JsonbProperty("besloten")
    val besloten: Boolean?,

    @field:JsonbProperty("heropend")
    val heropend: Boolean?
)
