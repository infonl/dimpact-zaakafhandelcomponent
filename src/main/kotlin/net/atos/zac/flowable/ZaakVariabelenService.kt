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
        const val VAR_ZAAK_UUID = "zaakUUID"
        const val VAR_ZAAK_IDENTIFICATIE = "zaakIdentificatie"
        const val VAR_ZAAKTYPE_UUUID = "zaaktypeUUID"
        const val VAR_ZAAKTYPE_OMSCHRIJVING = "zaaktypeOmschrijving"
        const val VAR_ONTVANGSTBEVESTIGING_VERSTUURD = "ontvangstbevestigingVerstuurd"
        const val VAR_DATUMTIJD_OPGESCHORT = "datumTijdOpgeschort"
        const val VAR_VERWACHTE_DAGEN_OPGESCHORT = "verwachteDagenOpgeschort"
        const val VAR_ZAAK_USER = "zaakBehandelaar"
        const val VAR_ZAAK_GROUP = "zaakGroep"
        private const val VAR_ONTVANKELIJK = "ontvankelijk"

        val ALL_ZAAK_VARIABLE_NAMES = listOf(
            VAR_ZAAK_UUID,
            VAR_ZAAK_IDENTIFICATIE,
            VAR_ZAAK_USER,
            VAR_ZAAK_GROUP,
            VAR_ZAAKTYPE_UUUID,
            VAR_ZAAKTYPE_OMSCHRIJVING,
            VAR_ONTVANGSTBEVESTIGING_VERSTUURD,
            VAR_DATUMTIJD_OPGESCHORT,
            VAR_VERWACHTE_DAGEN_OPGESCHORT,
            VAR_ONTVANKELIJK
        )
    }

    /**
     * Deletes all CMMN case variables for the given zaakUUID.
     * Does not need to be called before deleting the CMMN case itself.
     * Also deletes historical variables.
     *
     * @param zaakUUID the zaak UUID
     */
    fun deleteAllCaseVariables(zaakUUID: UUID) {
        cmmnRuntimeService.createCaseInstanceQuery()
            .variableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .singleResult()?.let {
                cmmnRuntimeService.removeVariables(it.id, ALL_ZAAK_VARIABLE_NAMES)
            }
    }

    fun readZaakUUID(planItemInstance: PlanItemInstance): UUID =
        readCaseVariable(planItemInstance, VAR_ZAAK_UUID) as UUID

    fun readZaaktypeUUID(planItemInstance: PlanItemInstance) =
        readCaseVariable(planItemInstance, VAR_ZAAKTYPE_UUUID) as UUID

    fun findOntvangstbevestigingVerstuurd(zaakUUID: UUID): Boolean? =
        findCaseVariable(zaakUUID, VAR_ONTVANGSTBEVESTIGING_VERSTUURD)?.let {
            it as Boolean
        }

    fun setOntvangstbevestigingVerstuurd(zaakUUID: UUID, ontvangstbevestigingVerstuurd: Boolean) =
        setVariable(zaakUUID, VAR_ONTVANGSTBEVESTIGING_VERSTUURD, ontvangstbevestigingVerstuurd)

    fun setOntvankelijk(planItemInstance: PlanItemInstance, ontvankelijk: Boolean) =
        cmmnRuntimeService.setVariable(planItemInstance.caseInstanceId, VAR_ONTVANKELIJK, ontvankelijk)

    fun findDatumtijdOpgeschort(zaakUUID: UUID) =
        findCaseVariable(zaakUUID, VAR_DATUMTIJD_OPGESCHORT)?.let {
            it as ZonedDateTime
        }

    fun setDatumtijdOpgeschort(zaakUUID: UUID, datumtijOpgeschort: ZonedDateTime) =
        setVariable(zaakUUID, VAR_DATUMTIJD_OPGESCHORT, datumtijOpgeschort)

    fun removeDatumtijdOpgeschort(zaakUUID: UUID) =
        removeVariable(zaakUUID, VAR_DATUMTIJD_OPGESCHORT)

    fun findVerwachteDagenOpgeschort(zaakUUID: UUID) =
        findCaseVariable(zaakUUID, VAR_VERWACHTE_DAGEN_OPGESCHORT)?.let {
            it as Int
        }

    fun setVerwachteDagenOpgeschort(zaakUUID: UUID, verwachteDagenOpgeschort: Int) =
        setVariable(zaakUUID, VAR_VERWACHTE_DAGEN_OPGESCHORT, verwachteDagenOpgeschort)

    fun removeVerwachteDagenOpgeschort(zaakUUID: UUID) =
        removeVariable(zaakUUID, VAR_VERWACHTE_DAGEN_OPGESCHORT)

    fun setGroup(zaakUUID: UUID, group: String) =
        setVariable(zaakUUID, VAR_ZAAK_GROUP, group)

    fun setUser(zaakUUID: UUID, user: String) =
        setVariable(zaakUUID, VAR_ZAAK_USER, user)

    fun removeUser(zaakUUID: UUID) =
        removeVariable(zaakUUID, VAR_ZAAK_USER)

    fun readZaakdata(zaakUUID: UUID) =
        findVariables(zaakUUID) ?: emptyMap()

    fun readProcessZaakdata(zaakUUID: UUID) =
        findProcessVariables(zaakUUID) ?: emptyMap()

    fun setZaakdata(zaakUUID: UUID, zaakdata: Map<String, Any>) =
        setVariables(zaakUUID, zaakdata)

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

    private fun findCaseVariable(zaakUUID: UUID, variableName: String) =
        findCaseVariables(zaakUUID)?.get(variableName)

    private fun findCaseVariables(zaakUUID: UUID) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceBusinessKey(zaakUUID.toString())
            .includeCaseVariables()
            .singleResult()?.caseVariables
            ?: cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceBusinessKey(zaakUUID.toString())
                .includeCaseVariables()
                .singleResult()?.caseVariables

    private fun findProcessVariables(zaakUUID: UUID) =
        bpmnRuntimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(zaakUUID.toString())
            .includeProcessVariables()
            .singleResult()?.processVariables
            ?: bpmnHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .includeProcessVariables()
                .singleResult()?.processVariables

    private fun findVariables(zaakUUID: UUID) =
        findCaseVariables(zaakUUID) ?: findProcessVariables(zaakUUID)

    @Suppress("TooGenericExceptionThrown")
    private fun setVariable(zaakUUID: UUID, variableName: String, value: Any) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .variableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .singleResult()?.let {
                cmmnRuntimeService.setVariable(it.id, variableName, value)
            }
            ?: bpmnRuntimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()?.let {
                    bpmnRuntimeService.setVariable(it.id, variableName, value)
                }
            ?: throw RuntimeException("No case or process instance found for zaak with UUID: '$zaakUUID'")

    @Suppress("TooGenericExceptionThrown")
    private fun setVariables(zaakUUID: UUID, variables: Map<String, Any>) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceBusinessKey(zaakUUID.toString())
            .singleResult()?.let {
                cmmnRuntimeService.setVariables(it.id, variables)
            }
            ?: bpmnRuntimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()?.let {
                    bpmnRuntimeService.setVariables(it.id, variables)
                }
            ?: throw RuntimeException("No case or process instance found for zaak with UUID: '$zaakUUID'")

    private fun removeVariable(zaakUUID: UUID, variableName: String) {
        cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceBusinessKey(zaakUUID.toString())
            .singleResult()?.let {
                cmmnRuntimeService.removeVariable(it.id, variableName)
            }
            ?: bpmnRuntimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()?.let {
                    bpmnRuntimeService.removeVariable(it.id, variableName)
                }
    }
}
