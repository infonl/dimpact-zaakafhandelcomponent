/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.model

import jakarta.json.bind.annotation.JsonbProperty

class ORValidationError : ORError() {
    @field:JsonbProperty("invalid_params")
    var fieldValidationErrors: List<ORFieldValidationError>? = null
}
