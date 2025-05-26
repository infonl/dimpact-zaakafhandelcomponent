/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import net.atos.zac.admin.model.BrpDoelbindingen
import net.atos.zac.admin.model.ZaakafhandelParameters

class RestBrpDoelbindingen {
    var id: Long? = null
    var zaakafhandelParameters: RestZaakafhandelParameters? = null
    var zoekWaarde: String? = null
    var raadpleegWaarde: String? = null

    constructor() {
        // Default constructor
    }

    constructor(zoekWaarde: String, raadpleegWaarde: String) {
        this.zoekWaarde = zoekWaarde
        this.raadpleegWaarde = raadpleegWaarde
    }
}

fun BrpDoelbindingen.toRestBrpDoelbindingen(): RestBrpDoelbindingen =
    RestBrpDoelbindingen().apply {
        id = this@toRestBrpDoelbindingen.id
        zoekWaarde = this@toRestBrpDoelbindingen.zoekWaarde
        raadpleegWaarde = this@toRestBrpDoelbindingen.raadpleegWaarde
    }

fun RestBrpDoelbindingen.toBrpDoelbindingen(
    zaakafhandelParameters: ZaakafhandelParameters
): BrpDoelbindingen = BrpDoelbindingen().apply {
    id = this@toBrpDoelbindingen.id
    zoekWaarde = this@toBrpDoelbindingen.zoekWaarde
    raadpleegWaarde = this@toBrpDoelbindingen.raadpleegWaarde
    this.zaakafhandelParameters = zaakafhandelParameters
}
