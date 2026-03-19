/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_TASK_FORM_NOT_FOUND
import nl.info.zac.exception.ZacSetupException

class BpmnTaskFormNotFoundException(override val message: String) : ZacSetupException(message, ERROR_CODE_BPMN_TASK_FORM_NOT_FOUND)
