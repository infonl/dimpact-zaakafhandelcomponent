/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult

data class NotitieRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val lezen: Boolean,
    @param:JsonbProperty("wijzigen") val wijzigen: Boolean
) : OpaRuleResult
