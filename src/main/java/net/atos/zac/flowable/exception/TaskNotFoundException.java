package net.atos.zac.flowable.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(final String message) {
        super(message);
    }

    public TaskNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
