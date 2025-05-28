/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.converter

import jakarta.inject.Inject
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.ZaakafhandelParameters
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.app.planitems.model.RESTPlanItem
import nl.info.zac.app.planitems.model.UserEventListenerActie
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.time.LocalDate
import java.util.UUID

class RESTPlanItemConverter @Inject constructor(
    val zaakafhandelParameterService: ZaakafhandelParameterService
) {
    fun convertPlanItems(planItems: List<PlanItemInstance>, zaak: Zaak): List<RESTPlanItem> =
        zaak.zaaktype.extractUuid().let { zaaktypeUUID ->
            zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID).let { zaakafhandelParameters ->
                planItems.map { convertPlanItem(it, zaak.uuid, zaakafhandelParameters) }
            }
        }

    fun convertPlanItem(
        planItem: PlanItemInstance,
        zaakUuid: UUID,
        zaakafhandelParameters: ZaakafhandelParameters
    ): RESTPlanItem =
        RESTPlanItem(
            id = planItem.id,
            naam = planItem.name,
            type = convertDefinitionType(planItem.planItemDefinitionType),
            zaakUuid = zaakUuid
        ).apply {
            when (type) {
                PlanItemType.USER_EVENT_LISTENER -> convertUserEventListener(this, planItem, zaakafhandelParameters)
                PlanItemType.HUMAN_TASK -> convertHumanTask(this, planItem, zaakafhandelParameters)
                PlanItemType.PROCESS_TASK -> {}
            }
        }

    private fun convertUserEventListener(
        restPlanItem: RESTPlanItem,
        userEventListenerPlanItem: PlanItemInstance,
        zaakafhandelParameters: ZaakafhandelParameters
    ): RESTPlanItem =
        restPlanItem.apply {
            userEventListenerActie = UserEventListenerActie.valueOf(userEventListenerPlanItem.planItemDefinitionId)
            toelichting = zaakafhandelParameters.readUserEventListenerParameters(
                userEventListenerPlanItem.planItemDefinitionId
            ).toelichting
        }

    private fun convertHumanTask(
        restPlanItem: RESTPlanItem,
        humanTaskPlanItem: PlanItemInstance,
        zaakafhandelParameters: ZaakafhandelParameters
    ): RESTPlanItem =
        restPlanItem.apply {
            zaakafhandelParameters.findHumanTaskParameter(humanTaskPlanItem.planItemDefinitionId).ifPresent {
                actief = it.isActief
                formulierDefinitie = FormulierDefinitie.valueOf(it.formulierDefinitieID)
                it.referentieTabellen.forEach { rt ->
                    tabellen[rt.veld] = rt.tabel.values.map(ReferenceTableValue::name)
                }
                groepId = it.groepID
                if (it.doorlooptijd != null) {
                    fataleDatum = LocalDate.now().plusDays(it.doorlooptijd.toLong())
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
