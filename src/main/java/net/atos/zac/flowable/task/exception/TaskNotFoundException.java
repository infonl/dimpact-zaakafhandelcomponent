/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.task.exception;

import jakarta.ws.rs.NotFoundException;

public class TaskNotFoundException extends NotFoundException {
    public TaskNotFoundException(final String message) {
        super(message);
    }

    public TaskNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
