/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
import net.atos.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_INTAKE
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.processengine.ProcessEngineLookupImpl.getCmmnEngineConfiguration
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.runtime.PlanItemInstance
import org.flowable.engine.delegate.TaskListener
import org.flowable.task.api.TaskInfo
import org.flowable.task.service.delegate.BaseTaskListener.EVENTNAME_COMPLETE
import org.flowable.task.service.delegate.BaseTaskListener.EVENTNAME_CREATE
import org.flowable.task.service.delegate.DelegateTask
import java.util.logging.Logger

@NoArgConstructor
class AanvullendeInformatieTaskListener : TaskListener {

    companion object {
        private val LOG = Logger.getLogger(AanvullendeInformatieTaskListener::class.java.name)

        private const val STATUS_TOELICHTING = "Status gewijzigd"
        private const val TASK_AANVULLENDE_INFORMATIE_ID = "AANVULLENDE_INFORMATIE"
    }

    override fun notify(delegateTask: DelegateTask) {
        LOG.fine {
            "AanvullendeInformatie task ${delegateTask.id} in state ${delegateTask.state} was ${delegateTask.eventName}"
        }
        with(delegateTask as TaskInfo) {
            when (delegateTask.eventName) {
                EVENTNAME_CREATE -> createdEvent(subScopeId)
                EVENTNAME_COMPLETE -> completedEvent(scopeId, subScopeId)
            }
        }
    }

    private fun createdEvent(subScopeId: String) {
        updateZaakStatusForTask(getPlanItemInstance(subScopeId), STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE)
    }

    private fun completedEvent(scopeId: String, subScopeId: String) {
        if (numberOfAdditionalInfoTasks(scopeId) == 1) {
            updateZaakStatusForTask(getPlanItemInstance(subScopeId), STATUSTYPE_OMSCHRIJVING_INTAKE)
        }
    }

    private fun getPlanItemInstance(subScopeId: String): PlanItemInstance =
        getCmmnEngineConfiguration().getCmmnRuntimeService()
            .createPlanItemInstanceQuery()
            .planItemInstanceId(subScopeId)
            .singleResult()

    private fun numberOfAdditionalInfoTasks(scopeId: String) =
        getCmmnEngineConfiguration().getCmmnRuntimeService()
            .createPlanItemInstanceQuery()
            .caseInstanceId(scopeId)
            .planItemDefinitionId(TASK_AANVULLENDE_INFORMATIE_ID)
            .planItemInstanceState("active")
            .list().size

    private fun updateZaakStatusForTask(planItemInstance: PlanItemInstance, statustypeOmschrijving: String) =
        getZaak(planItemInstance).let { zaak ->
            getStatustypeDescription(zaak).takeIf { it != statustypeOmschrijving }?.let {
                updateZaakStatus(zaak, statustypeOmschrijving)
            }
        }

    private fun getZaak(planItemInstance: PlanItemInstance) =
        withFlowableHelper { flowableHelper ->
            flowableHelper.zaakVariabelenService.readZaakUUID(planItemInstance).let { zaakUUID ->
                flowableHelper.zrcClientService.readZaak(zaakUUID)
            }
        }

    private fun getStatustypeDescription(zaak: Zaak): String =
        withFlowableHelper { flowableHelper ->
            flowableHelper.zrcClientService.readStatus(zaak.status).let { zaakStatus ->
                flowableHelper.ztcClientService.readStatustype(zaakStatus.statustype).omschrijving
            }
        }

    private fun updateZaakStatus(zaak: Zaak, statustypeOmschrijving: String) =
        withFlowableHelper { flowableHelper ->
            LOG.info("Zaak with UUID '${zaak.uuid}': changing status to '$statustypeOmschrijving'")
            flowableHelper.zgwApiService.createStatusForZaak(
                zaak,
                statustypeOmschrijving,
                STATUS_TOELICHTING
            )
        }

    inline fun <T> withFlowableHelper(block: (FlowableHelper) -> T): T = block(FlowableHelper.getInstance())
}
