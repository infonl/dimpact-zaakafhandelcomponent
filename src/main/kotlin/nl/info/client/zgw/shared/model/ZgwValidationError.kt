/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import java.net.URI

/**
 * ZGW error indicating a validation error.
 * The structure of these errors comply to the <a href="https://datatracker.ietf.org/doc/html/rfc7807">Problem Details Standard</a>.
 */
class ZgwValidationError @JsonbCreator constructor(
    @param:JsonbProperty("type") type: URI?,
    @param:JsonbProperty("code") code: String?,
    @param:JsonbProperty("title") title: String?,
    @param:JsonbProperty("status") status: Int,
    @param:JsonbProperty("detail") detail: String?,
    @param:JsonbProperty("instance") instance: URI?,
    @param:JsonbProperty("invalidParams") val invalidParams: List<FieldValidationError>
) : ZgwError(type, code, title, status, detail, instance) {
    companion object {
        private const val serialVersionUID: Long = 79823432543535L
    }

    override fun toString() = buildString {
        append(super.toString()).append("\n")
        invalidParams.forEach { append(it.toString()).append("\n") }
    }
}
