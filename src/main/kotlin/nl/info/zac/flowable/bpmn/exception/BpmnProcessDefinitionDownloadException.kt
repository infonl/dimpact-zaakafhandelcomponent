/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_DOWNLOAD_FAILED
import nl.info.zac.exception.ServerErrorException

class BpmnProcessDefinitionDownloadException(message: String, cause: Throwable) : ServerErrorException(
    errorCode = ERROR_CODE_BPMN_PROCESS_DEFINITION_DOWNLOAD_FAILED,
    message = message,
    cause = cause,
)
