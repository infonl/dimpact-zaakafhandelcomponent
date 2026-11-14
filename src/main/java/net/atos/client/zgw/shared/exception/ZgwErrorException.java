/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.exception;

import java.net.URI;

import net.atos.client.zgw.shared.model.ZgwError;

/**
 * Exception thrown when an error occurred in the ZGW APIs.
 */
public class ZgwErrorException extends RuntimeException {

    private final ZgwError zgwError;

    public ZgwErrorException(final ZgwError zgwError) {
        this.zgwError = zgwError;
    }

    public ZgwError getZgwError() {
        return zgwError;
    }

    @Override
    public String getMessage() {
        return "%s [%d %s] %s (%s %s)"
                .formatted(
                        zgwError.getTitle(),
                        zgwError.getStatus(),
                        zgwError.getCode(),
                        zgwError.getDetail(),
                        uri(zgwError.getType()),
                        uri(zgwError.getInstance()));
    }

    private String uri(final URI uri) {
        return uri == null ? null : uri.toString();
    }
}
