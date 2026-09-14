/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class ZaakAssignmentCannotBeChangedByUpdateException : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_ZAAK_ASSIGNMENT_CANNOT_BE_CHANGED_BY_UPDATE
)
