/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class WerklijstRechten @JsonbCreator constructor(
    @param:JsonbProperty("inbox") val inbox: Boolean,
    @param:JsonbProperty("ontkoppelde_documenten_verwijderen") val ontkoppeldeDocumentenVerwijderen: Boolean,
    @param:JsonbProperty("inbox_productaanvragen_verwijderen") val inboxProductaanvragenVerwijderen: Boolean,
    @param:JsonbProperty("zaken_taken") val zakenTaken: Boolean,
    @param:JsonbProperty("zaken_taken_verdelen") val zakenTakenVerdelen: Boolean,
    @param:JsonbProperty("zaken_taken_exporteren") val zakenTakenExporteren: Boolean
) : OpaRuleResult
