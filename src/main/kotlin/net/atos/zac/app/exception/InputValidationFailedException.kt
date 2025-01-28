/*
* SPDX-FileCopyrightText: 2024 Lifely
* SPDX-License-Identifier: EUPL-1.2+
*/
package net.atos.zac.app.exception

import jakarta.ws.rs.BadRequestException
import nl.info.zac.exception.ErrorCode

/**
 * Custom exception for input validation failures.
 * We subclass from the JAX-RS [BadRequestException] class so that we can use our generic exception handling mechanism
 */
class InputValidationFailedException(message: String) : BadRequestException(message) {
    constructor(errorCode: ErrorCode) : this(errorCode.value)
}
