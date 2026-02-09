/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBrpDoelbindingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaakafhandelParameters? = null,
    var zoekWaarde: String? = null,
    var raadpleegWaarde: String? = null,
    var verwerkingregisterWaarde: String? = null
)

fun ZaaktypeBrpParameters.toRestBrpDoelbindingen() = RestBrpDoelbindingen().apply {
    id = this@toRestBrpDoelbindingen.id
    zoekWaarde = this@toRestBrpDoelbindingen.zoekWaarde
    raadpleegWaarde = this@toRestBrpDoelbindingen.raadpleegWaarde
    verwerkingregisterWaarde = this@toRestBrpDoelbindingen.verwerkingregisterWaarde
}

fun RestBrpDoelbindingen.toZaaktypeBrpParameters(
    zaaktypeConfiguration: ZaaktypeConfiguration
) = ZaaktypeBrpParameters().apply {
    id = this@toZaaktypeBrpParameters.id
    zoekWaarde = this@toZaaktypeBrpParameters.zoekWaarde
    raadpleegWaarde = this@toZaaktypeBrpParameters.raadpleegWaarde
    verwerkingregisterWaarde = this@toZaaktypeBrpParameters.verwerkingregisterWaarde
    this.zaaktypeConfiguration = zaaktypeConfiguration
}
