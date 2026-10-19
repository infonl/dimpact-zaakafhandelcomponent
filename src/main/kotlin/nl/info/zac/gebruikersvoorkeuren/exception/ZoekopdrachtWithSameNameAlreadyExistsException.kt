/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.gebruikersvoorkeuren.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class ZoekopdrachtWithSameNameAlreadyExistsException(message: String) : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_ZOEKOPDRACHT_WITH_SAME_NAME_ALREADY_EXISTS,
    message = message
)
