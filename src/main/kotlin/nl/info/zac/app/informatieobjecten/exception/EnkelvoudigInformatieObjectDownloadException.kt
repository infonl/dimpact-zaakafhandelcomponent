/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.informatieobjecten.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_ENKELVOUDIGINFORMATIEOBJECT_DOWNLOAD_FAILED
import nl.info.zac.exception.ServerErrorException

class EnkelvoudigInformatieObjectDownloadException(message: String, cause: Throwable) : ServerErrorException(
    message = message,
    cause = cause,
    errorCode = ERROR_CODE_ENKELVOUDIGINFORMATIEOBJECT_DOWNLOAD_FAILED,
)
