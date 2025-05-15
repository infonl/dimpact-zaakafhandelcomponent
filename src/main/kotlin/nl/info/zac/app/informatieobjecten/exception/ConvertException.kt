/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.informatieobjecten.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_OPEN_SUBCASES
import nl.info.zac.exception.InputValidationFailedException

class ConvertException() : InputValidationFailedException(
    errorCode = ERROR_CODE_CASE_HAS_OPEN_SUBCASES,
)
