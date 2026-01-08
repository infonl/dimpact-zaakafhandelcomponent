/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.shared.model;

import java.io.Serial;
import java.io.Serializable;

public class FieldValidationError implements Serializable {
    @Serial
    private static final long serialVersionUID = 7808093454353L;

    private String name;

    private String code;

    private String reason;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }
}
