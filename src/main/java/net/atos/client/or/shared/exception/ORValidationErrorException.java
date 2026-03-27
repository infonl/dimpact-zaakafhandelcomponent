/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.shared.exception;

import java.net.URI;
import java.util.stream.Collectors;

import net.atos.client.or.shared.model.ORValidationError;
import nl.info.zac.exception.InputValidationFailedException;

public class ORValidationErrorException extends InputValidationFailedException {

    private final ORValidationError validatieFout;

    public ORValidationErrorException(final ORValidationError validatieFout) {
        this.validatieFout = validatieFout;
    }

    public ORValidationError getValidatieFout() {
        return validatieFout;
    }

    @Override
    public String getMessage() {
        return "%s [%d %s] %s: %s (%s)"
                .formatted(
                        validatieFout.getTitle(),
                        validatieFout.getStatus(),
                        validatieFout.getCode(),
                        validatieFout.getDetail(),
                        validatieFout.getFieldValidationErrors().stream()
                                .map(error -> "%s [%s] %s"
                                        .formatted(error.getName(),
                                                error.getCode(),
                                                error.getReason()))
                                .collect(Collectors.joining(", ")),
                        uri(validatieFout.getInstance())
                );
    }

    private String uri(final URI uri) {
        return uri == null ? null : uri.toString();
    }
}
