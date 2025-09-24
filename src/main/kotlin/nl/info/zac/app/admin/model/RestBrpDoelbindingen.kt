/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import net.atos.zac.admin.model.ZaaktypeCmmnBrpParameters
import net.atos.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestBrpDoelbindingen(
    var id: Long? = null,
    var zaakafhandelParameters: RestZaakafhandelParameters? = null,
    var zoekWaarde: String? = null,
    var raadpleegWaarde: String? = null
)

fun ZaaktypeCmmnBrpParameters.toRestBrpDoelbindingen(): RestBrpDoelbindingen =
    RestBrpDoelbindingen().apply {
        id = this@toRestBrpDoelbindingen.id
        zoekWaarde = this@toRestBrpDoelbindingen.zoekWaarde
        raadpleegWaarde = this@toRestBrpDoelbindingen.raadpleegWaarde
    }

fun RestBrpDoelbindingen.toBrpDoelbindingen(
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
): ZaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
    id = this@toBrpDoelbindingen.id
    zoekWaarde = this@toBrpDoelbindingen.zoekWaarde
    raadpleegWaarde = this@toBrpDoelbindingen.raadpleegWaarde
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
}
