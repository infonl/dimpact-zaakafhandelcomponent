/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.delegate.log.ZaakExtending
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.log.Employee
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.log.PolicyDenied
import nl.info.zac.websocket.event.ScreenEventType
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

/**
 * Flowable BPMN delegate to extend a zaak.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
class ExtendZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var aantalDagen: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var verlengingReden: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var takenVerlengen: Expression? = null

    companion object {
        private val LOG = ZacLogger.getLogger(ExtendZaakDelegate::class)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val (zaak, zaaktype) = flowableHelper.zaakService.readZaakAndZaakTypeByZaakID(getZaakIdentificatie(execution))
        val loggedInUser = flowableHelper.loggedInUserInstance.get()
        val zaakRechten = flowableHelper.policyService.readZaakRechten(
            zaak = zaak,
            zaaktype = zaaktype,
            loggedInUser = loggedInUser
        )
        assertPolicy(
            zaakRechten.canVerlengen,
            LOG,
            PolicyDenied(action = "extendZaak", zaakIdentificatie = zaak.identificatie, employee = Employee(loggedInUser.username))
        )

        val numberOfDays = aantalDagen.resolveValueAsInt(execution)
        LOG.fine(
            ZaakExtending(
                zaakIdentificatie = zaak.identificatie,
                activityName = execution.currentActivityName,
                numberOfDays = numberOfDays
            )
        )
        val updatedZaak = flowableHelper.suspensionZaakHelper.extendZaak(
            zaak = zaak,
            zaakType = zaaktype,
            extensionReason = verlengingReden.resolveValueAsString(execution),
            numberOfDays = numberOfDays
        )

        if (takenVerlengen?.resolveValueAsBoolean(execution) ?: false) {
            flowableHelper.suspensionZaakHelper.extendTasks(zaak, numberOfDays)
                .forEach { flowableHelper.eventingService.send(ScreenEventType.TAAK.updated(it)) }
                .also { flowableHelper.eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak)) }
        }
    }
}
