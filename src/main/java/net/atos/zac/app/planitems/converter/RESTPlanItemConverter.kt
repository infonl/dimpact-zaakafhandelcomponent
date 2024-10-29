/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.HumanTaskParameters
import net.atos.zac.admin.model.HumanTaskReferentieTabel
import net.atos.zac.admin.model.ReferenceTableValue
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.app.planitems.model.PlanItemType
import net.atos.zac.app.planitems.model.RESTPlanItem
import net.atos.zac.app.planitems.model.UserEventListenerActie
import net.atos.zac.util.UriUtil
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.time.LocalDate
import java.util.UUID
import java.util.function.Consumer

/**
 *
 */
class RESTPlanItemConverter {
    @Inject
    private val zaakafhandelParameterService: ZaakafhandelParameterService? = null

    fun convertPlanItems(planItems: List<PlanItemInstance>, zaak: Zaak): List<RESTPlanItem?> {
        val zaaktypeUUID = UriUtil.uuidFromURI(zaak.zaaktype)
        val zaakafhandelParameters = zaakafhandelParameterService!!.readZaakafhandelParameters(
            zaaktypeUUID
        )
        return planItems.stream()
            .map { planItemInstance: PlanItemInstance ->
                this.convertPlanItem(
                    planItemInstance,
                    zaak.uuid,
                    zaakafhandelParameters
                )
            }
            .toList()
    }

    fun convertPlanItem(
        planItem: PlanItemInstance,
        zaakUuid: UUID?,
        zaakafhandelParameters: ZaakafhandelParameters
    ): RESTPlanItem {
        val restPlanItem = RESTPlanItem()
        restPlanItem.id = planItem.id
        restPlanItem.naam = planItem.name
        restPlanItem.zaakUuid = zaakUuid
        restPlanItem.type = convertDefinitionType(planItem.planItemDefinitionType)
        return when (restPlanItem.type) {
            PlanItemType.USER_EVENT_LISTENER -> convertUserEventListener(restPlanItem, planItem, zaakafhandelParameters)
            PlanItemType.HUMAN_TASK -> convertHumanTask(restPlanItem, planItem, zaakafhandelParameters)
            PlanItemType.PROCESS_TASK -> convertProcessTask(restPlanItem)
        }
    }

    private fun convertUserEventListener(
        restPlanItem: RESTPlanItem,
        UserEventListenerPlanItem: PlanItemInstance,
        zaakafhandelParameters: ZaakafhandelParameters
    ): RESTPlanItem {
        restPlanItem.userEventListenerActie = UserEventListenerActie.valueOf(
            UserEventListenerPlanItem.planItemDefinitionId
        )
        restPlanItem.toelichting = zaakafhandelParameters.readUserEventListenerParameters(
            UserEventListenerPlanItem.planItemDefinitionId
        ).toelichting
        return restPlanItem
    }

    private fun convertHumanTask(
        restPlanItem: RESTPlanItem,
        humanTaskPlanItem: PlanItemInstance,
        zaakafhandelParameters: ZaakafhandelParameters
    ): RESTPlanItem {
        zaakafhandelParameters.findHumanTaskParameter(humanTaskPlanItem.planItemDefinitionId)
            .ifPresent { humanTaskParameters: HumanTaskParameters ->
                restPlanItem.actief = humanTaskParameters.isActief
                restPlanItem.formulierDefinitie =
                    FormulierDefinitie.valueOf(humanTaskParameters.formulierDefinitieID)
                humanTaskParameters.referentieTabellen.forEach(
                    Consumer { rt: HumanTaskReferentieTabel ->
                        restPlanItem.tabellen[rt.veld] = rt.tabel.values.stream()
                            .map(ReferenceTableValue::name)
                            .toList()
                    })
                restPlanItem.groepId = humanTaskParameters.groepID
                if (humanTaskParameters.doorlooptijd != null) {
                    restPlanItem.fataleDatum = LocalDate.now().plusDays(humanTaskParameters.doorlooptijd.toLong())
                }
            }
        return restPlanItem
    }

    private fun convertProcessTask(restPlanItem: RESTPlanItem): RESTPlanItem {
        return restPlanItem
    }

    companion object {
        private fun convertDefinitionType(planItemDefinitionType: String): PlanItemType {
            return if (PlanItemDefinitionType.HUMAN_TASK == planItemDefinitionType) {
                PlanItemType.HUMAN_TASK
            } else if (PlanItemDefinitionType.PROCESS_TASK == planItemDefinitionType) {
                PlanItemType.PROCESS_TASK
            } else if (PlanItemDefinitionType.USER_EVENT_LISTENER == planItemDefinitionType) {
                PlanItemType.USER_EVENT_LISTENER
            } else {
                throw IllegalArgumentException(
                    String.format(
                        "Conversie van plan item definition type '%s' wordt niet ondersteund",
                        planItemDefinitionType
                    )
                )
            }
        }
    }
}
