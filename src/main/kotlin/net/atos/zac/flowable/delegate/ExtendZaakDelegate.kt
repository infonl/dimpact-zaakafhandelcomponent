/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.websocket.event.ScreenEventType
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class ExtendZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var aantalDagen: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var einddatumGepland: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var uiterlijkeEinddatumAfdoening: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var verlengingReden: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var takenVerlengen: Expression? = null

    companion object {
        private val LOG = Logger.getLogger(ExtendZaakDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        LOG.fine(
            "Extending zaak '${zaak.identificatie}' from activity '${execution.currentActivityName}' " +
                "for $aantalDagen days with reason '$verlengingReden' with planned end date '$einddatumGepland' and " +
                "latest settlement date '$uiterlijkeEinddatumAfdoening'"
        )

        val numberOfDays = aantalDagen.resolveValueAsInt(execution)
        val updatedZaak = flowableHelper.suspensionZaakHelper.extendZaak(
            zaak = zaak,
            plannedEndDate = einddatumGepland?.resolveValueAsLocalDate(execution),
            latestSettlementDate = uiterlijkeEinddatumAfdoening?.resolveValueAsLocalDate(execution),
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
