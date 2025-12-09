/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.CmmnHistoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.runtime.PlanItemInstance
import org.flowable.engine.HistoryService
import org.flowable.engine.RuntimeService
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.UUID

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
open class ZaakVariabelenService @Inject constructor(
    private val cmmnRuntimeService: CmmnRuntimeService,
    private val cmmnHistoryService: CmmnHistoryService,
    private val bpmnRuntimeService: RuntimeService,
    private val bpmnHistoryService: HistoryService
) {

    companion object {
        const val VAR_DATUMTIJD_OPGESCHORT = "datumTijdOpgeschort"
        const val VAR_ONTVANGSTBEVESTIGING_VERSTUURD = "ontvangstbevestigingVerstuurd"
        private const val VAR_ONTVANKELIJK = "ontvankelijk"
        const val VAR_VERWACHTE_DAGEN_OPGESCHORT = "verwachteDagenOpgeschort"
        const val VAR_ZAAK_USER = "zaakBehandelaar"
        const val VAR_ZAAK_COMMUNICATIEKANAAL = "zaakCommunicatiekanaal"
        const val VAR_ZAAK_GROUP = "zaakGroep"
        const val VAR_ZAAK_IDENTIFICATIE = "zaakIdentificatie"
        const val VAR_ZAAK_UUID = "zaakUUID"
        const val VAR_ZAAKTYPE_OMSCHRIJVING = "zaaktypeOmschrijving"
        const val VAR_ZAAKTYPE_UUID = "zaaktypeUUID"

        val ALL_ZAAK_VARIABLE_NAMES = listOf(
            VAR_DATUMTIJD_OPGESCHORT,
            VAR_ONTVANGSTBEVESTIGING_VERSTUURD,
            VAR_ONTVANKELIJK,
            VAR_VERWACHTE_DAGEN_OPGESCHORT,
            VAR_ZAAK_USER,
            VAR_ZAAK_COMMUNICATIEKANAAL,
            VAR_ZAAK_GROUP,
            VAR_ZAAK_IDENTIFICATIE,
            VAR_ZAAK_UUID,
            VAR_ZAAKTYPE_OMSCHRIJVING,
            VAR_ZAAKTYPE_UUID,
        )
    }

    /**
     * Deletes all CMMN case variables for the given zaakUuid.
     * Does not need to be called before deleting the CMMN case itself.
     * Also deletes historical variables.
     *
     * @param zaakUuid the zaak UUID
     */
    fun deleteAllCaseVariables(zaakUuid: UUID) {
        cmmnRuntimeService.createCaseInstanceQuery()
            .variableValueEquals(VAR_ZAAK_UUID, zaakUuid)
            .singleResult()?.let {
                cmmnRuntimeService.removeVariables(it.id, ALL_ZAAK_VARIABLE_NAMES)
            }
    }

    fun readZaakUUID(planItemInstance: PlanItemInstance): UUID =
        readCaseVariable(planItemInstance, VAR_ZAAK_UUID) as UUID

    fun readZaaktypeUUID(planItemInstance: PlanItemInstance) =
        readCaseVariable(planItemInstance, VAR_ZAAKTYPE_UUID) as UUID

    fun findOntvangstbevestigingVerstuurd(zaakUuid: UUID): Boolean? =
        findVariables(zaakUuid)?.get(VAR_ONTVANGSTBEVESTIGING_VERSTUURD)?.let {
            it as Boolean
        }

    fun setOntvangstbevestigingVerstuurd(zaakUuid: UUID, ontvangstbevestigingVerstuurd: Boolean) =
        setVariable(zaakUuid, VAR_ONTVANGSTBEVESTIGING_VERSTUURD, ontvangstbevestigingVerstuurd)

    fun setOntvankelijk(planItemInstance: PlanItemInstance, ontvankelijk: Boolean) =
        cmmnRuntimeService.setVariable(planItemInstance.caseInstanceId, VAR_ONTVANKELIJK, ontvankelijk)

    fun findDatumtijdOpgeschort(zaakUuid: UUID) =
        findVariables(zaakUuid)?.get(VAR_DATUMTIJD_OPGESCHORT)?.let {
            when (it) {
                is String -> ZonedDateTime.parse(it)
                else -> it as ZonedDateTime
            }
        }

    fun setDatumtijdOpgeschort(zaakUuid: UUID, datumtijOpgeschort: ZonedDateTime) =
        setVariable(zaakUuid, VAR_DATUMTIJD_OPGESCHORT, datumtijOpgeschort)

    fun removeDatumtijdOpgeschort(zaakUuid: UUID) =
        removeVariable(zaakUuid, VAR_DATUMTIJD_OPGESCHORT)

    fun findVerwachteDagenOpgeschort(zaakUuid: UUID) =
        findVariables(zaakUuid)?.get(VAR_VERWACHTE_DAGEN_OPGESCHORT)?.let {
            when (it) {
                is BigDecimal -> it.toInt()
                else -> it as Int
            }
        }

    fun setVerwachteDagenOpgeschort(zaakUuid: UUID, verwachteDagenOpgeschort: Int) =
        setVariable(zaakUuid, VAR_VERWACHTE_DAGEN_OPGESCHORT, verwachteDagenOpgeschort)

    fun removeVerwachteDagenOpgeschort(zaakUuid: UUID) =
        removeVariable(zaakUuid, VAR_VERWACHTE_DAGEN_OPGESCHORT)

    fun setGroup(zaakUuid: UUID, group: String) =
        setVariable(zaakUuid, VAR_ZAAK_GROUP, group)

    fun setUser(zaakUuid: UUID, user: String) =
        setVariable(zaakUuid, VAR_ZAAK_USER, user)

    fun removeUser(zaakUuid: UUID) =
        removeVariable(zaakUuid, VAR_ZAAK_USER)

    fun setCommunicatiekanaal(zaakUuid: UUID, communicatiekanaal: String) =
        setVariable(zaakUuid, VAR_ZAAK_COMMUNICATIEKANAAL, communicatiekanaal)

    fun readZaakdata(zaakUuid: UUID) =
        findVariables(zaakUuid) ?: emptyMap()

    fun readProcessZaakdata(zaakUuid: UUID) =
        findProcessVariables(zaakUuid) ?: emptyMap()

    fun setZaakdata(zaakUuid: UUID, zaakdata: Map<String, Any>) =
        setVariables(zaakUuid, zaakdata)

    @Suppress("TooGenericExceptionThrown")
    private fun readCaseVariable(planItemInstance: PlanItemInstance, variableName: String) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceId(planItemInstance.caseInstanceId)
            .includeCaseVariables()
            .singleResult()?.caseVariables[variableName]
            ?: cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(planItemInstance.caseInstanceId)
                .includeCaseVariables()
                .singleResult()?.caseVariables[variableName]
            ?: throw RuntimeException(
                "No variable found with name '$variableName' for case instance id '${planItemInstance.caseInstanceId}'"
            )

    private fun findCaseVariables(zaakUuid: UUID) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceBusinessKey(zaakUuid.toString())
            .includeCaseVariables()
            .singleResult()?.caseVariables
            ?: cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceBusinessKey(zaakUuid.toString())
                .includeCaseVariables()
                .singleResult()?.caseVariables

    private fun findProcessVariables(zaakUuid: UUID) =
        bpmnRuntimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(zaakUuid.toString())
            .includeProcessVariables()
            .singleResult()?.processVariables
            ?: bpmnHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUuid.toString())
                .includeProcessVariables()
                .singleResult()?.processVariables

    private fun findVariables(zaakUuid: UUID) =
        findCaseVariables(zaakUuid) ?: findProcessVariables(zaakUuid)

    @Suppress("TooGenericExceptionThrown")
    private fun setVariable(zaakUuid: UUID, variableName: String, value: Any) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .variableValueEquals(VAR_ZAAK_UUID, zaakUuid)
            .singleResult()?.let {
                cmmnRuntimeService.setVariable(it.id, variableName, value)
            }
            ?: bpmnRuntimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUuid.toString())
                .singleResult()?.let {
                    bpmnRuntimeService.setVariable(it.id, variableName, value)
                }
            ?: throw RuntimeException("No case or process instance found for zaak with UUID: '$zaakUuid'")

    @Suppress("TooGenericExceptionThrown")
    private fun setVariables(zaakUuid: UUID, variables: Map<String, Any>) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceBusinessKey(zaakUuid.toString())
            .singleResult()?.let {
                cmmnRuntimeService.setVariables(it.id, variables)
            }
            ?: bpmnRuntimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUuid.toString())
                .singleResult()?.let {
                    bpmnRuntimeService.setVariables(it.id, variables)
                }
            ?: throw RuntimeException("No case or process instance found for zaak with UUID: '$zaakUuid'")

    private fun removeVariable(zaakUuid: UUID, variableName: String) {
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceBusinessKey(zaakUuid.toString())
            .singleResult()?.let {
                cmmnRuntimeService.removeVariable(it.id, variableName)
            }
            ?: bpmnRuntimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUuid.toString())
                .singleResult()?.let {
                    bpmnRuntimeService.removeVariable(it.id, variableName)
                }
    }
}
