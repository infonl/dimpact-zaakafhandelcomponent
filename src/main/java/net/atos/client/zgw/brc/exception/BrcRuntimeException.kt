/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.brc.exception;

import net.atos.client.zgw.shared.exception.ZgwRuntimeException;

public class BrcRuntimeException extends ZgwRuntimeException {
    public BrcRuntimeException(final String message) {
        super(message);
    }
}
