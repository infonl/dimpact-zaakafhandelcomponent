/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.admin.model.RestHumanTaskParameters
import nl.info.zac.app.admin.model.RestHumanTaskReferenceTable
import nl.info.zac.app.admin.model.RestPlanItemDefinition
import nl.info.zac.app.planitems.converter.readFormulierVeldDefinities
import nl.info.zac.app.planitems.converter.toFormulierDefinitie

class RestHumanTaskParametersConverter @Inject constructor(
    private val restHumanTaskReferenceTableConverter: RestHumanTaskReferenceTableConverter
) {
    fun convertHumanTaskParametersCollection(
        zaaktypeCmmnHumantaskParametersCollection: Collection<ZaaktypeCmmnHumantaskParameters>,
        humanTaskDefinitions: List<RestPlanItemDefinition>
    ) = humanTaskDefinitions.map { humanTaskDefinition ->
        zaaktypeCmmnHumantaskParametersCollection
            .find { it.planItemDefinitionID == humanTaskDefinition.id }
            ?.toRestHumanTaskParameters(humanTaskDefinition)
            ?: humanTaskDefinition.toDefaultRestHumanTaskParameters()
    }

    fun convertRestHumanTaskParameters(restHumanTaskParametersList: List<RestHumanTaskParameters>) =
        restHumanTaskParametersList.map { it.toZaaktypeCmmnHumantaskParameters() }

    private fun ZaaktypeCmmnHumantaskParameters.toRestHumanTaskParameters(
        humanTaskDefinition: RestPlanItemDefinition
    ) = RestHumanTaskParameters(
        id = id,
        isActief = isActief,
        defaultGroepId = groepID,
        planItemDefinition = humanTaskDefinition,
        formulierDefinitieId = getFormulierDefinitieID(),
        doorlooptijd = doorlooptijd,
        referentieTabellen = convertReferentieTabellen(this, humanTaskDefinition)
    )

    private fun convertReferentieTabellen(
        zaaktypeCmmnHumantaskParameters: ZaaktypeCmmnHumantaskParameters,
        humanTaskDefinition: RestPlanItemDefinition
    ): List<RestHumanTaskReferenceTable> {
        val referentieTabellen = restHumanTaskReferenceTableConverter.toRestHumanTaskReferenceTables(
            zaaktypeCmmnHumantaskParameters.getReferentieTabellen()
        )
        val veldenWithReferentieTabel = referentieTabellen.map { it.veld }.toSet()
        return referentieTabellen + humanTaskDefinition.id.readFormulierVeldDefinities()
            .filter { it.name !in veldenWithReferentieTabel }
            .map(restHumanTaskReferenceTableConverter::convertDefault)
    }

    private fun RestHumanTaskParameters.toZaaktypeCmmnHumantaskParameters() =
        ZaaktypeCmmnHumantaskParameters().apply {
            val restHumanTaskParameters = this@toZaaktypeCmmnHumantaskParameters
            id = restHumanTaskParameters.id
            isActief = restHumanTaskParameters.isActief
            doorlooptijd = restHumanTaskParameters.doorlooptijd
            planItemDefinitionID = restHumanTaskParameters.planItemDefinition.id
            groepID = restHumanTaskParameters.defaultGroepId
            setFormulierDefinitieID(restHumanTaskParameters.formulierDefinitieId)
            setReferentieTabellen(
                restHumanTaskReferenceTableConverter.toHumanTaskReferentieTabellen(
                    restHumanTaskParameters.referentieTabellen
                )
            )
        }

    private fun RestPlanItemDefinition.toDefaultRestHumanTaskParameters() = RestHumanTaskParameters(
        planItemDefinition = this,
        isActief = false,
        formulierDefinitieId = id.toFormulierDefinitie().name
    )
}
