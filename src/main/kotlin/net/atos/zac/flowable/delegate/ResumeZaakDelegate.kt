/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class ResumeZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var hervattenReden: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var hervattenDatum: Expression? = null

    companion object {
        private val LOG = Logger.getLogger(ResumeZaakDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))
        val resumeReason = hervattenReden.resolveValueAsString(execution)
        val resumeDate = hervattenDatum?.resolveValueAsZonedDateTime(execution)

        LOG.fine(
            "Resuming zaak '${zaak.identificatie}' from activity '${execution.currentActivityName}' " +
                "with reason '$resumeReason' ${resumeDate?.let { "and resume date '$it'" } ?: ""}"
        )

        resumeDate?.let {
            flowableHelper.suspensionZaakHelper.resumeZaak(zaak, resumeReason, resumeDate)
        } ?: flowableHelper.suspensionZaakHelper.resumeZaak(zaak, resumeReason)
    }
}
