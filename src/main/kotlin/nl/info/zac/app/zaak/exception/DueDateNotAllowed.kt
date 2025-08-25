/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_CANNOT_HAVE_DUE_DATE
import nl.info.zac.exception.InputValidationFailedException

class DueDateNotAllowed : InputValidationFailedException(
    errorCode = ERROR_CODE_CASE_CANNOT_HAVE_DUE_DATE,
)
