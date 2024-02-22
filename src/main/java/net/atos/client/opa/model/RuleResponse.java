/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.opa.model;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.DummyInterface;

public class RuleResponse<T extends DummyInterface> {

    private final T result;

    @JsonbCreator
    public RuleResponse(@JsonbProperty("result") final T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }
}
