/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

package nl.info.zac.zaak.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
import nl.info.zac.exception.InputValidationFailedException

class CaseHasLockedInformationObjectsException(message: String) : InputValidationFailedException(
    errorCode = ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS,
    message = message
)
