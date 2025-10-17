/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class PostDelegate : AbstractDelegate() {
    // set by Flowable
    private lateinit var template: FixedValue

    companion object {
        private val LOG: Logger = Logger.getLogger(PostDelegate::class.java.name)
    }

    override fun execute(delegateExecution: DelegateExecution) {
        LOG.info(
            "Verstuur per post besluit van zaak '${getZaakIdentificatie(delegateExecution)}' via " +
                "${PostDelegate::class.java.simpleName} met template '${template.expressionText}'."
        )
    }
}
