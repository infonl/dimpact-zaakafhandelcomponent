package net.atos.zac.flowable.exception;

import jakarta.ws.rs.NotFoundException;

public class TaskNotFoundException extends NotFoundException {
    public TaskNotFoundException(final String message) {
        super(message);
    }

    public TaskNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
