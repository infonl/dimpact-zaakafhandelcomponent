/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.shared

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

fun String.toBrondatumEigenschap(): LocalDate =
    try {
        ZonedDateTime.parse(this).toLocalDate()
    } catch (exception: DateTimeParseException) {
        throw InputValidationFailedException(
            errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
            message = "Brondatum eigenschap '$this' kon niet worden geparsed als een datum."
        )
    }
