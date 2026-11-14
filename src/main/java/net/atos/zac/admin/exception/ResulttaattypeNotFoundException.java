/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.exception;

import jakarta.ws.rs.NotFoundException;

public class ResulttaattypeNotFoundException extends NotFoundException {
    public ResulttaattypeNotFoundException(final String message) {
        super(message);
    }
}
