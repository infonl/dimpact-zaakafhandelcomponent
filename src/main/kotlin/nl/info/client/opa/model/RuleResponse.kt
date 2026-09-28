/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.opa.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty

/**
 * OPA's Data API omits the 'result' field entirely when the queried rule path is undefined,
 * e.g. because the rule was renamed or removed from the policy bundle.
 */
data class RuleResponse<T : OpaRuleResult> @JsonbCreator constructor(
    @param:JsonbProperty("result") val result: T?
)
