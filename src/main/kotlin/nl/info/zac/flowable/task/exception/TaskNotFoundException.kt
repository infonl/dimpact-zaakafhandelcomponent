/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.flowable.task.exception

import jakarta.ws.rs.NotFoundException

class TaskNotFoundException(message: String) : NotFoundException(message) {
    constructor(message: String, cause: Throwable) : this(message) {
        initCause(cause)
    }
}
