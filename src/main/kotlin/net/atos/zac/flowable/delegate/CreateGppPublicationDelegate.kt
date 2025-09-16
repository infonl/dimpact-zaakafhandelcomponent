/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class CreateGppPublicationDelegate : AbstractDelegate() {
    companion object {
        private val LOG = Logger.getLogger(CreateGppPublicationDelegate::class.java.name)
    }

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var aantalDagen: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var opschortingReden: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var selectedDocuments: Expression

    override fun execute(execution: DelegateExecution) {
        createGppPublication(execution)
        suspendZaak(execution)
    }

    private fun createGppPublication(execution: DelegateExecution) {
        // TODO Implement GPP publication creation logic here
        LOG.info {
            "Creating GPP publication for zaak '${getZaakIdentificatie(execution)}' " +
                "and selected documents: '${selectedDocuments.resolveValueAsList(execution)}'."
        }
    }

    private fun suspendZaak(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))
        LOG.info(
            "Suspending zaak '${zaak.identificatie}' from activity '${execution.currentActivityName}' " +
                "for $aantalDagen days with reason '$opschortingReden'"
        )
        flowableHelper.suspensionZaakHelper.suspendZaak(
            zaak = zaak,
            numberOfDays = aantalDagen.resolveValueAsLong(execution),
            suspensionReason = opschortingReden.resolveValueAsString(execution)
        )
    }
}
