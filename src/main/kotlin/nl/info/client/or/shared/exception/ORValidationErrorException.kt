/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import nl.info.client.or.shared.model.ORValidationError
import nl.info.zac.exception.InputValidationFailedException

class ORValidationErrorException(val validatieFout: ORValidationError) : InputValidationFailedException() {
    override val message: String
        get() = "${validatieFout.title} [${validatieFout.status} ${validatieFout.code}] ${validatieFout.detail}: " +
            "${validatieFout.fieldValidationErrors?.joinToString(", ") { error -> "${error.name} [${error.code}] ${error.reason}" }} " +
            "(${validatieFout.instance})"
}
