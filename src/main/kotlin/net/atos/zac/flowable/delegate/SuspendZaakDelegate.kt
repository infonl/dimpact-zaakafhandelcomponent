/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.delegate.log.ZaakSuspending
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.log.PolicyDenied
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

/**
 * Flowable BPMN delegate to suspend a zaak.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
class SuspendZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var aantalDagen: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var opschortingReden: Expression

    companion object {
        private val LOG = ZacLogger.getLogger(SuspendZaakDelegate::class)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))
        val loggedInUser = flowableHelper.loggedInUserInstance.get()
        assertPolicy(
            flowableHelper.policyService.readZaakRechten(zaak, loggedInUser).canOpschorten,
            LOG,
            PolicyDenied(action = "suspendZaak", zaakIdentificatie = zaak.identificatie)
        )

        val numberOfDays = aantalDagen.resolveValueAsLong(execution)
        LOG.fine(
            ZaakSuspending(
                zaakIdentificatie = zaak.identificatie,
                activityName = execution.currentActivityName,
                numberOfDays = numberOfDays
            )
        )

        flowableHelper.suspensionZaakHelper.suspendZaak(
            zaak = zaak,
            numberOfDays = numberOfDays,
            suspensionReason = opschortingReden.resolveValueAsString(execution)
        )
    }
}
