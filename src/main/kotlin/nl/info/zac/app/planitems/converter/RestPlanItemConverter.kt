/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.converter

import jakarta.inject.Inject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.admin.model.ZaaktypeCmmnExtension
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.app.planitems.model.RestPlanItem
import nl.info.zac.app.planitems.model.UserEventListenerActie
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.time.LocalDate
import java.util.UUID

class RestPlanItemConverter @Inject constructor(
    val zaaktypeConfigurationService: ZaaktypeConfigurationService
) {
    fun convertPlanItems(planItems: List<PlanItemInstance>, zaak: Zaak): List<RestPlanItem> =
        zaak.zaaktype.extractUuid().let { zaaktypeUUID ->
            zaaktypeConfigurationService.findConfiguration(zaaktypeUUID)?.cmmnExtension.let { cmmnExtension ->
                planItems.map { convertPlanItem(it, zaak.uuid, cmmnExtension) }
            }
        }

    fun convertPlanItem(
        planItem: PlanItemInstance,
        zaakUuid: UUID,
        zaaktypeCmmnExtension: ZaaktypeCmmnExtension?
    ): RestPlanItem =
        RestPlanItem(
            id = planItem.id,
            naam = planItem.name,
            type = convertDefinitionType(planItem.planItemDefinitionType),
            zaakUuid = zaakUuid
        ).apply {
            when (type) {
                PlanItemType.USER_EVENT_LISTENER -> convertUserEventListener(this, planItem, zaaktypeCmmnExtension)
                PlanItemType.HUMAN_TASK -> convertHumanTask(this, planItem, zaaktypeCmmnExtension)
                PlanItemType.PROCESS_TASK -> {}
            }
        }

    @Suppress("TooGenericExceptionThrown")
    private fun convertUserEventListener(
        restPlanItem: RestPlanItem,
        userEventListenerPlanItem: PlanItemInstance,
        zaaktypeCmmnExtension: ZaaktypeCmmnExtension?
    ): RestPlanItem =
        restPlanItem.apply {
            userEventListenerActie = UserEventListenerActie.valueOf(userEventListenerPlanItem.planItemDefinitionId)
            toelichting = (
                zaaktypeCmmnExtension ?: throw RuntimeException(
                    "No UserEventListenerParameters found for planitemDefinitionID: " +
                        "'${userEventListenerPlanItem.planItemDefinitionId}', because the zaaktype has no CMMN configuration"
                )
                ).readUserEventListenerParameters(userEventListenerPlanItem.planItemDefinitionId).toelichting
        }

    @Suppress("ExplicitItLambdaParameter")
    private fun convertHumanTask(
        restPlanItem: RestPlanItem,
        humanTaskPlanItem: PlanItemInstance,
        zaaktypeCmmnExtension: ZaaktypeCmmnExtension?
    ): RestPlanItem =
        restPlanItem.apply {
            zaaktypeCmmnExtension
                ?.findHumanTaskParameter(humanTaskPlanItem.planItemDefinitionId)
                ?.let { it ->
                    isActief = it.isActief
                    it.getFormulierDefinitieID()?.let { fd ->
                        formulierDefinitie = FormulierDefinitie.valueOf(fd)
                    }
                    it.getReferentieTabellen().forEach { rt ->
                        val tabel = rt.tabel ?: return@forEach
                        tabellen[rt.veld] = tabel.values.map(ReferenceTableValue::name)
                    }
                    groepId = it.groepID
                    it.doorlooptijd?.let { days ->
                        fataleDatum = LocalDate.now().plusDays(days.toLong())
                    }
                }
        }

    private fun convertDefinitionType(planItemDefinitionType: String): PlanItemType =
        when (planItemDefinitionType) {
            PlanItemDefinitionType.HUMAN_TASK -> PlanItemType.HUMAN_TASK
            PlanItemDefinitionType.PROCESS_TASK -> PlanItemType.PROCESS_TASK
            PlanItemDefinitionType.USER_EVENT_LISTENER -> PlanItemType.USER_EVENT_LISTENER
            else -> throw IllegalArgumentException(
                "Conversie van plan item definition type '$planItemDefinitionType' wordt niet ondersteund"
            )
        }
}
