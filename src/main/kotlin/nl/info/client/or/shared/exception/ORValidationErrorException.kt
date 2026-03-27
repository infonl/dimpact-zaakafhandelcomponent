/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import nl.info.client.or.shared.model.ORValidationError
import nl.info.zac.exception.InputValidationFailedException

class ORValidationErrorException(orValidationError: ORValidationError) : InputValidationFailedException(
    message = "${orValidationError.title} [${orValidationError.status} ${orValidationError.code}] ${orValidationError.detail}: " +
        "${orValidationError.fieldValidationErrors?.joinToString(", ") { error -> "${error.name} [${error.code}] ${error.reason}" }} " +
        "(${orValidationError.instance})"
)
