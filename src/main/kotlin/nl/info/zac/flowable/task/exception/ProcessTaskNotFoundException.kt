/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_PROCESS_TASK_NOT_FOUND
import nl.info.zac.exception.ZacSetupException

class ProcessTaskNotFoundException(message: String) : ZacSetupException(message, ERROR_CODE_PROCESS_TASK_NOT_FOUND)
