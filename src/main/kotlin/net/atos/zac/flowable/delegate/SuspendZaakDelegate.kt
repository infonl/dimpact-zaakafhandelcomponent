/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class SuspendZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var aantalDagen: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var opschortingReden: Expression

    companion object {
        private val LOG = Logger.getLogger(SuspendZaakDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        LOG.fine(
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
