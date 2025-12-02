/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBrpDoelbindingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaakafhandelParameters? = null,
    var zoekWaarde: String? = null,
    var raadpleegWaarde: String? = null,
    var verwerkingregisterWaarde: String? = null
)

fun ZaaktypeBrpParameters.toRestBrpDoelbindingen(): RestBrpDoelbindingen =
    RestBrpDoelbindingen().apply {
        id = this@toRestBrpDoelbindingen.id
        zoekWaarde = this@toRestBrpDoelbindingen.zoekWaarde
        raadpleegWaarde = this@toRestBrpDoelbindingen.raadpleegWaarde
        verwerkingregisterWaarde = this@toRestBrpDoelbindingen.verwerkingregisterWaarde
    }

fun RestBrpDoelbindingen.toBrpDoelbindingen(
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
): ZaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
    id = this@toBrpDoelbindingen.id
    zoekWaarde = this@toBrpDoelbindingen.zoekWaarde
    raadpleegWaarde = this@toBrpDoelbindingen.raadpleegWaarde
    verwerkingregisterWaarde = this@toBrpDoelbindingen.verwerkingregisterWaarde
    this.zaaktypeConfiguration = zaaktypeCmmnConfiguration
}
