/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult

data class DocumentRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val lezen: Boolean,
    @param:JsonbProperty("wijzigen") val wijzigen: Boolean,
    @param:JsonbProperty("verwijderen") val verwijderen: Boolean,
    @param:JsonbProperty("vergrendelen") val vergrendelen: Boolean,
    @param:JsonbProperty("ontgrendelen") val ontgrendelen: Boolean,
    @param:JsonbProperty("ondertekenen") val ondertekenen: Boolean,
    @param:JsonbProperty("toevoegen_nieuwe_versie") val toevoegenNieuweVersie: Boolean,
    @param:JsonbProperty("verplaatsen") val verplaatsen: Boolean,
    @param:JsonbProperty("ontkoppelen") val ontkoppelen: Boolean,
    @param:JsonbProperty("downloaden") val downloaden: Boolean
) : OpaRuleResult
