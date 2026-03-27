/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.shared.exception;

import java.net.URI;

import net.atos.client.or.shared.model.ORError;

public class ORErrorException extends RuntimeException {

    private final ORError ORError;

    public ORErrorException(final ORError ORError) {
        this.ORError = ORError;
    }

    public ORError getFout() {
        return ORError;
    }

    @Override
    public String getMessage() {
        return "%s [%d %s] %s (%s)"
                .formatted(ORError.getTitle(),
                        ORError.getStatus(),
                        ORError.getCode(),
                        ORError.getDetail(),
                        uri(ORError.getInstance()));
    }

    private String uri(final URI uri) {
        return uri == null ? null : uri.toString();
    }
}
