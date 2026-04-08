/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.informatieobjecten.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_DETACHED_DOCUMENT_NOT_FOUND
import nl.info.zac.exception.ServerErrorException

class DetachedDocumentNotFoundException(message: String) : ServerErrorException(
    message = message,
    errorCode = ERROR_CODE_DETACHED_DOCUMENT_NOT_FOUND,
)
