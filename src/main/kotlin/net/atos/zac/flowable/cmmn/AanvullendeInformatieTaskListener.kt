package net.atos.zac.flowable.cmmn

import org.flowable.engine.delegate.TaskListener
import org.flowable.task.service.delegate.DelegateTask
import java.util.logging.Logger

class AanvullendeInformatieTaskListener: TaskListener {

    companion object {
        private val LOG = Logger.getLogger(AanvullendeInformatieTaskListener::class.java.name)
    }

    override fun notify(delegateTask: DelegateTask) {
        LOG.fine {
            "AanvullendeInformatie task ${delegateTask.id} in state ${delegateTask.state} was ${delegateTask.eventName}"
        };
        when (delegateTask.eventName) {
            "create" -> createdEvent()
            "completed" -> completedEvent()
        }
    }

    private fun createdEvent() {
        // Update zaak status
    }

    private fun completedEvent() {
        // Check if this is the last task
    }
}
