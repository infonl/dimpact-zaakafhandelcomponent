/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.informatieobjecten.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_ENKELVOUDIGINFORMATIEOBJECT_CONVERSION_FAILED
import nl.info.zac.exception.InputValidationFailedException

class EnkelvoudigInformatieObjectConversionException : InputValidationFailedException(
    errorCode = ERROR_CODE_ENKELVOUDIGINFORMATIEOBJECT_CONVERSION_FAILED,
)
