/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.model;

import static java.lang.String.format;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import org.jetbrains.annotations.NotNull;

/**
 * ZGW field validation error message indicating that a field in a ZGW API request does not meet the
 * expected validation requirements.
 *
 * @param name   Name of the field with invalid data
 * @param code   System code indicating the type of error
 * @param reason Explanation of what is specifically wrong with the data (in Dutch)
 */
public record FieldValidationError(String name, String code, String reason) {

    @JsonbCreator
    public FieldValidationError(
            @JsonbProperty("name") final String name,
            @JsonbProperty("code") final String code,
            @JsonbProperty("reason") final String reason
    ) {
        this.name = name;
        this.code = code;
        this.reason = reason;
    }

    @Override
    public @NotNull String toString() {
        return format("Name: %s, Code: %s, Reason: %s", name, code, reason);
    }
}
