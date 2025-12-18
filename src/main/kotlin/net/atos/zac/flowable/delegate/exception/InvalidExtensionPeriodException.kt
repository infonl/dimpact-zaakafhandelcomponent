/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package net.atos.zac.flowable.delegate.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_EXTENSION_PERIOD_INVALID
import nl.info.zac.exception.InputValidationFailedException

class InvalidExtensionPeriodException(message: String) : InputValidationFailedException(
    errorCode = ERROR_CODE_EXTENSION_PERIOD_INVALID,
    message = message
)
