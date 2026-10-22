/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestUserEventListenerParameter(
    var id: String,
    var naam: String? = null,
    var toelichting: String? = null
)

fun RestPlanItemDefinition.toRestUserEventListenerParameter(
    zaaktypeCmmnUsereventlistenerParametersCollection: Collection<ZaaktypeCmmnUsereventlistenerParameters>
) = RestUserEventListenerParameter(
    id = id,
    naam = naam,
    toelichting = zaaktypeCmmnUsereventlistenerParametersCollection
        .find { it.planItemDefinitionID == id }
        ?.toelichting
)

fun RestUserEventListenerParameter.toZaaktypeCmmnUsereventlistenerParameters() =
    ZaaktypeCmmnUsereventlistenerParameters().apply {
        planItemDefinitionID = this@toZaaktypeCmmnUsereventlistenerParameters.id
        toelichting = this@toZaaktypeCmmnUsereventlistenerParameters.toelichting
    }
