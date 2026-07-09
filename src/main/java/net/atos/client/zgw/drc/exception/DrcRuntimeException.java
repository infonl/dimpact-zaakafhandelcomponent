/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.drc.exception;

import nl.info.client.zgw.shared.exception.ZgwRuntimeException;

public class DrcRuntimeException extends ZgwRuntimeException {
    public DrcRuntimeException(final String message) {
        super(message);
    }
}
