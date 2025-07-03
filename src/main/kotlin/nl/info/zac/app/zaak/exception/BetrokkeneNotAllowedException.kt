/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class BetrokkeneNotAllowedException : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED
)
