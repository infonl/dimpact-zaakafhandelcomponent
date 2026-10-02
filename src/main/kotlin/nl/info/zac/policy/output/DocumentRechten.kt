/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult

data class DocumentRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val canLezen: Boolean,
    @param:JsonbProperty("wijzigen") val canWijzigen: Boolean,
    @param:JsonbProperty("verwijderen") val canVerwijderen: Boolean,
    @param:JsonbProperty("vergrendelen") val canVergrendelen: Boolean,
    @param:JsonbProperty("ontgrendelen") val canOntgrendelen: Boolean,
    @param:JsonbProperty("ondertekenen") val canOndertekenen: Boolean,
    @param:JsonbProperty("toevoegen_nieuwe_versie") val canToevoegenNieuweVersie: Boolean,
    @param:JsonbProperty("verplaatsen") val canVerplaatsen: Boolean,
    @param:JsonbProperty("ontkoppelen") val canOntkoppelen: Boolean,
    @param:JsonbProperty("downloaden") val canDownloaden: Boolean,
    @param:JsonbProperty("converteren") val canConverteren: Boolean
) : OpaRuleResult
