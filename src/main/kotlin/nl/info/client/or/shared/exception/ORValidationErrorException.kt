/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import nl.info.client.or.shared.model.ORValidationError
import nl.info.zac.exception.InputValidationFailedException

/**
 * The message holds only the status, the error code and the name and code of each invalid field. Not the reasons, nor
 * the title, detail and instance of the remote error: those can echo a rejected value, which can be personal data, and
 * this message ends up in logs.
 */
class ORValidationErrorException(orValidationError: ORValidationError) : InputValidationFailedException(
    message = "${orValidationError.status} ${orValidationError.code}: " +
        orValidationError.fieldValidationErrors.orEmpty().joinToString(", ") { "${it.name} [${it.code}]" }
)
