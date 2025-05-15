/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED
import nl.info.zac.exception.InputValidationFailedException

class BetrokkeneNotAllowed : InputValidationFailedException(
    errorCode = ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED,
)
