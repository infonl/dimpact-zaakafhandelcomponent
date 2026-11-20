/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.time.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun String.toValidatedLocalDate(): LocalDate =
    try {
        toLocalDate()
    } catch (dateTimeParseException: DateTimeParseException) {
        throw InputValidationFailedException(
            errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
            message = "Waarde '$this' kon niet worden geparsed als een datum."
        )
    }
