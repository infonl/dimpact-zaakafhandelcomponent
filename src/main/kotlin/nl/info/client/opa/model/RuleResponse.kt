/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.opa.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.util.SerializableByYasson

data class RuleResponse<T : SerializableByYasson> @JsonbCreator constructor(
    @param:JsonbProperty("result") val result: T
)
