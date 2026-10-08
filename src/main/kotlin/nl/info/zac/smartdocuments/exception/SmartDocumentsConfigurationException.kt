/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED
import nl.info.zac.exception.ServerErrorException

open class SmartDocumentsConfigurationException(message: String? = null) : ServerErrorException(
    errorCode = ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED,
    message = message ?: "SmartDocuments is not configured correctly"
)
