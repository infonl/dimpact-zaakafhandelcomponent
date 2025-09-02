package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class ResumeZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var hervattenReden: Expression

    companion object {
        private val LOG: Logger = Logger.getLogger(SendEmailDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        LOG.info(
            "Resuming zaak '${zaak.identificatie}' from activity '${execution.currentActivityName}' " +
                    "with reason '$hervattenReden'"
        )

        flowableHelper.suspensionZaakHelper.resumeZaak(
            zaak = zaak,
            resumeReason = hervattenReden.resolveValueAsString(execution)
        )
    }
}
