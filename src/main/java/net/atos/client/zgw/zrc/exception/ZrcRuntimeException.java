/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.exception;

import nl.info.client.zgw.shared.exception.ZgwRuntimeException;

public class ZrcRuntimeException extends ZgwRuntimeException {
    public ZrcRuntimeException(final String message) {
        super(message);
    }
}
