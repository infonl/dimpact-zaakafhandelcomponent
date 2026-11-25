/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.delegate.log.ZaakNotSuspended
import net.atos.zac.flowable.delegate.log.ZaakResuming
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.log.PolicyDenied
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

/**
 * Flowable BPMN delegate to resume a zaak.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
class ResumeZaakDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var hervattenReden: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var hervattenDatum: Expression? = null

    companion object {
        private val LOG = ZacLogger.getLogger(ResumeZaakDelegate::class)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))
        val loggedInUser = flowableHelper.loggedInUserInstance.get()
        assertPolicy(
            flowableHelper.policyService.readZaakRechten(zaak, loggedInUser).canHervatten,
            LOG,
            PolicyDenied(action = "resumeZaak", zaakIdentificatie = zaak.identificatie)
        )
        assertPolicy(zaak.isOpgeschort(), LOG, ZaakNotSuspended(zaakIdentificatie = zaak.identificatie))
        val resumeReason = hervattenReden.resolveValueAsString(execution)
        val resumeDate = hervattenDatum?.resolveValueAsZonedDateTime(execution)

        LOG.fine(
            ZaakResuming(
                zaakIdentificatie = zaak.identificatie,
                activityName = execution.currentActivityName,
                resumeDate = resumeDate
            )
        )

        resumeDate?.let {
            flowableHelper.suspensionZaakHelper.resumeZaak(zaak, resumeReason, resumeDate)
        } ?: flowableHelper.suspensionZaakHelper.resumeZaak(zaak, resumeReason)
    }
}
