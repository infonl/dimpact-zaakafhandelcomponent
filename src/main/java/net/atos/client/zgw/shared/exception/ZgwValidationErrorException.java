/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.exception;

import java.net.URI;
import java.util.stream.Collectors;

import net.atos.client.zgw.shared.model.ValidationZgwError;
import nl.info.zac.exception.InputValidationFailedException;

/**
 * Exception to indicate a validation error that occurred in when calling the ZGW API.
 */
public class ZgwValidationErrorException extends InputValidationFailedException {

    private final ValidationZgwError validatieFout;

    public ZgwValidationErrorException(final ValidationZgwError validationZgwError) {
        this.validatieFout = validationZgwError;
    }

    public ValidationZgwError getValidatieFout() {
        return validatieFout;
    }

    @Override
    public String getMessage() {
        return "%s [%d %s] %s: %s (%s %s)"
                .formatted(validatieFout.getTitle(),
                        validatieFout.getStatus(),
                        validatieFout.getCode(),
                        validatieFout.getDetail(),
                        validatieFout.getInvalidParams().stream()
                                .map(error -> "%s [%s] %s"
                                        .formatted(
                                                error.name(),
                                                error.code(),
                                                error.reason()
                                        )
                                )
                                .collect(Collectors.joining(", ")),
                        uri(validatieFout.getType()),
                        uri(validatieFout.getInstance()));
    }

    private String uri(final URI uri) {
        return uri == null ? null : uri.toString();
    }
}
