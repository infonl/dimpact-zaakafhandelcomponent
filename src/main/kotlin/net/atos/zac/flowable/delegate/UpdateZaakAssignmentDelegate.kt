/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class UpdateZaakAssignmentDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var groepId: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var behandelaarGebruikersnaam: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var reden: Expression? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(UpdateZaakAssignmentDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        val groupId = groepId.resolveValueAsString(execution)
        val userId = behandelaarGebruikersnaam?.resolveValueAsString(execution)
        val reason = reden?.resolveValueAsString(execution)

        LOG.info { "Updating zaak ${zaak.identificatie} assignment with group '$groupId', user '$userId', reason '$reason'" }
        flowableHelper.zaakService.assignZaak(zaak, groupId, userId, reason)
    }
}
