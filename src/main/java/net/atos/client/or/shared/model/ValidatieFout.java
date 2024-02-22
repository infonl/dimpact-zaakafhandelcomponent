/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.shared.model;

import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 *
 */
public class ValidatieFout extends Fout {

    @JsonbProperty("invalid_params")
    private List<FieldValidationError> fieldValidationErrors;

    public List<FieldValidationError> getFieldValidationErrors() {
        return fieldValidationErrors;
    }

    public void setFieldValidationErrors(final List<FieldValidationError> fieldValidationErrors) {
        this.fieldValidationErrors = fieldValidationErrors;
    }
}
