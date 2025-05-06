/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_INITIATOR_NOT_ALLOWED
import nl.info.zac.exception.InputValidationFailedException

class InitiatorNotAllowed : InputValidationFailedException(
    errorCode = ERROR_CODE_CASE_INITIATOR_NOT_ALLOWED,
)
