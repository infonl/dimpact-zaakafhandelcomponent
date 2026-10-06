/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult

data class WerklijstRechten @JsonbCreator constructor(
    @param:JsonbProperty("inbox") val canInbox: Boolean,
    @param:JsonbProperty("ontkoppelde_documenten_verwijderen") val canOntkoppeldeDocumentenVerwijderen: Boolean,
    @param:JsonbProperty("inbox_productaanvragen_verwijderen") val canInboxProductaanvragenVerwijderen: Boolean,
    @param:JsonbProperty("zaken_taken") val canZakenTaken: Boolean,
    @param:JsonbProperty("zaken_taken_verdelen") val canZakenTakenVerdelen: Boolean,
    @param:JsonbProperty("zaken_taken_exporteren") val canZakenTakenExporteren: Boolean
) : OpaRuleResult
