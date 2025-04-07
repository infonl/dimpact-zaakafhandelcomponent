/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED
import nl.info.zac.exception.InputValidationFailedException

open class SmartDocumentsConfigurationException(message: String? = null) : InputValidationFailedException(
    errorCode = ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED,
    message = message
)
