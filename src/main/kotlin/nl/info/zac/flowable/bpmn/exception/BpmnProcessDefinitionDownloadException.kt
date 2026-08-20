/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_DOWNLOAD_FAILED
import nl.info.zac.exception.ServerErrorException

/**
 * The error code of this exception does not reach the browser: a download is requested as a blob,
 * so its error body arrives as a blob as well and cannot be read as an error code. Callers report
 * the failure themselves; the code is what the response carries for any other client.
 */
class BpmnProcessDefinitionDownloadException(message: String, cause: Throwable) : ServerErrorException(
    errorCode = ERROR_CODE_BPMN_PROCESS_DEFINITION_DOWNLOAD_FAILED,
    message = message,
    cause = cause,
)
