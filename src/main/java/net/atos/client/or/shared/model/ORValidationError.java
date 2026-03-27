/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.shared.model;

import java.io.Serial;
import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

public class ORValidationError extends ORError {
    @Serial
    private static final long serialVersionUID = 456455676575665L;

    @JsonbProperty("invalid_params")
    private List<ORFieldValidationError> ORFieldValidationErrors;

    public List<ORFieldValidationError> getFieldValidationErrors() {
        return ORFieldValidationErrors;
    }

    public void setFieldValidationErrors(final List<ORFieldValidationError> ORFieldValidationErrors) {
        this.ORFieldValidationErrors = ORFieldValidationErrors;
    }
}
