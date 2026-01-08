/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.shared.model;

import java.io.Serial;
import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

public class ValidatieFout extends Fout {
    @Serial
    private static final long serialVersionUID = 456455676575665L;

    @JsonbProperty("invalid_params")
    private List<FieldValidationError> fieldValidationErrors;

    public List<FieldValidationError> getFieldValidationErrors() {
        return fieldValidationErrors;
    }

    public void setFieldValidationErrors(final List<FieldValidationError> fieldValidationErrors) {
        this.fieldValidationErrors = fieldValidationErrors;
    }
}
