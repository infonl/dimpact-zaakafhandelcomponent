/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty

/**
 * ZGW field validation error message indicating that a field in a ZGW API request does not meet the
 * expected validation requirements.
 *
 * @param name Name of the field with invalid data
 * @param code System code indicating the type of error
 * @param reason Explanation of what is specifically wrong with the data (in Dutch)
 */
data class FieldValidationError @JsonbCreator constructor(
    @param:JsonbProperty("name") val name: String,
    @param:JsonbProperty("code") val code: String,
    @param:JsonbProperty("reason") val reason: String
) {
    override fun toString() = "Name: $name, Code: $code, Reason: $reason"
}
