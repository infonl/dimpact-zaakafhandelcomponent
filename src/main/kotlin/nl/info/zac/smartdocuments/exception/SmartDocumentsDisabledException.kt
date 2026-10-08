/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_DISABLED
import nl.info.zac.exception.ServerErrorException

class SmartDocumentsDisabledException : ServerErrorException(
    errorCode = ERROR_CODE_SMARTDOCUMENTS_DISABLED,
    message = "SmartDocuments is disabled"
)
