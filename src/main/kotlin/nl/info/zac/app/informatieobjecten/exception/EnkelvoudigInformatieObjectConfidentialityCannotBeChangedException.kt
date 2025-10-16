/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.informatieobjecten.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class EnkelvoudigInformatieObjectConfidentialityCannotBeChangedException : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_CONFIDENTIALITY_CANNOT_BE_CHANGED,
)
