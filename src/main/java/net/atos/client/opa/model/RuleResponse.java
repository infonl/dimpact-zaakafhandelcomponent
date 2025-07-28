/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.opa.model;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.SerializableByYasson;

public record RuleResponse<T extends SerializableByYasson>(T result) {

    @JsonbCreator
    public RuleResponse(@JsonbProperty("result") final T result) {
        this.result = result;
    }
}
