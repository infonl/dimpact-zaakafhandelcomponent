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
    lateinit var group: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    val user: Expression? = null

    // Set by Flowable. Can be either FixedValue or JuelExpression
    val reason: Expression? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(UpdateZaakAssignmentDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        LOG.info {
            "Updating zaak ${zaak.identificatie} assignment with group '${group.expressionText}', " +
                "user '${user?.expressionText}', reason '${reason?.expressionText}'"
        }
        flowableHelper.zaakService.assignZaak(
            zaak,
            groupId = group.resolveValueAsString(execution),
            user = user?.resolveValueAsString(execution),
            reason = reason?.resolveValueAsString(execution)
        )
    }
}
