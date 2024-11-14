package net.atos.zac.flowable.cmmn

import net.atos.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
import net.atos.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_INTAKE
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.processengine.ProcessEngineLookupImpl.getCmmnEngineConfiguration
import nl.lifely.zac.util.NoArgConstructor
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
        (delegateTask as TaskInfo).let { taskInfo ->
            when (delegateTask.eventName) {
                EVENTNAME_CREATE -> createdEvent(taskInfo.subScopeId)
                EVENTNAME_COMPLETE -> completedEvent(taskInfo.scopeId, taskInfo.subScopeId)
            }
        }
    }

    private fun createdEvent(subScopeId: String) {
        updateZaak(getPlanItemInstance(subScopeId), STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE)
    }

    private fun completedEvent(scopeId: String, subScopeId: String) {
        if (numberOfAdditionalInfoTasks(scopeId) == 1) {
            updateZaak(getPlanItemInstance(subScopeId), STATUSTYPE_OMSCHRIJVING_INTAKE)
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

    private fun updateZaak(planItemInstance: PlanItemInstance, statustypeOmschrijving: String) =
        FlowableHelper.getInstance().let { flowableHelper ->
            flowableHelper.zaakVariabelenService.readZaakUUID(planItemInstance).let { zaakUUID ->
                flowableHelper.zrcClientService.readZaak(zaakUUID).let { zaak ->
                    LOG.info("Zaak with UUID '$zaakUUID': changing status to '$statustypeOmschrijving'")
                    flowableHelper.zgwApiService.createStatusForZaak(
                        zaak,
                        statustypeOmschrijving,
                        STATUS_TOELICHTING
                    )
                }
            }
        }
}
