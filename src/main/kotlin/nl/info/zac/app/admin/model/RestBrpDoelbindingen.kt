/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeCmmnBrpParameters
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBrpDoelbindingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaakafhandelParameters? = null,
    var zoekWaarde: String? = null,
    var raadpleegWaarde: String? = null,
    var verwerkingsregisterWaarde: String? = null
)

fun ZaaktypeCmmnBrpParameters.toRestBrpDoelbindingen(): RestBrpDoelbindingen =
    RestBrpDoelbindingen().apply {
        id = this@toRestBrpDoelbindingen.id
        zoekWaarde = this@toRestBrpDoelbindingen.zoekWaarde
        raadpleegWaarde = this@toRestBrpDoelbindingen.raadpleegWaarde
        verwerkingsregisterWaarde = this@toRestBrpDoelbindingen.verwerkingsregisterWaarde
    }

fun RestBrpDoelbindingen.toBrpDoelbindingen(
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
): ZaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
    id = this@toBrpDoelbindingen.id
    zoekWaarde = this@toBrpDoelbindingen.zoekWaarde
    raadpleegWaarde = this@toBrpDoelbindingen.raadpleegWaarde
    verwerkingsregisterWaarde = this@toBrpDoelbindingen.verwerkingsregisterWaarde
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
}
