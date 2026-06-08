/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import nl.info.zac.policy.assertPolicy
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

/**
 * Flowable BPMN delegate to update a zaak assignment.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
class UpdateZaakAssignmentDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var groepId: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var behandelaarGebruikersnaam: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var reden: Expression? = null

    companion object {
        private val LOG = Logger.getLogger(UpdateZaakAssignmentDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val (zaak, zaaktype) = flowableHelper.zaakService.readZaakAndZaakTypeByZaakID(getZaakIdentificatie(execution))
        val zaakRechten = flowableHelper.policyService.readZaakRechten(
            zaak,
            zaaktype,
            flowableHelper.loggedInUserInstance.get()
        )
        assertPolicy(
            zaakRechten.toekennen,
            LOG,
            "User ${flowableHelper.loggedInUserInstance.get().id} not allowed to assign zaak ${zaak.identificatie}"
        )

        val groupId = groepId.resolveValueAsString(execution)
        val userId = behandelaarGebruikersnaam?.resolveValueAsString(execution)
        val reason = reden?.resolveValueAsString(execution)

        LOG.fine { "Updating zaak ${zaak.identificatie} assignment with group '$groupId', user '$userId', reason '$reason'" }
        flowableHelper.zaakService.assignZaak(zaak, groupId, userId, reason)
    }
}
