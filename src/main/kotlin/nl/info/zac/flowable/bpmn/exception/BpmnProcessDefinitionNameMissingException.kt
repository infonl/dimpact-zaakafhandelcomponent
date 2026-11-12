/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_NAME_MISSING
import nl.info.zac.exception.InputValidationFailedException

class BpmnProcessDefinitionNameMissingException(message: String) : InputValidationFailedException(
    errorCode = ERROR_CODE_BPMN_PROCESS_DEFINITION_NAME_MISSING,
    message = message,
)
