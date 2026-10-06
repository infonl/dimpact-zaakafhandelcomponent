/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult

data class TaakRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val canLezen: Boolean,
    @param:JsonbProperty("wijzigen") val canWijzigen: Boolean,
    @param:JsonbProperty("toekennen") val canToekennen: Boolean,
    @param:JsonbProperty("creeren_document") val canCreerenDocument: Boolean,
    @param:JsonbProperty("toevoegen_document") val canToevoegenDocument: Boolean
) : OpaRuleResult
