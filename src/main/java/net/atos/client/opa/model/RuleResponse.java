/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.opa.model;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.SerializableByYasson;

public class RuleResponse<T extends SerializableByYasson> {

    private final T result;

    @JsonbCreator
    public RuleResponse(@JsonbProperty("result") final T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }
}
