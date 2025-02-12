/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.zac.flowable.FlowableHelper
import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener
import org.flowable.cmmn.api.runtime.CaseInstance
import java.util.UUID
import java.util.logging.Logger


/**
 * Custom Flowable end case lifecycle listener.
 */
class EndCaseLifecycleListener(
    private val sourceState: String,
    private val targetState: String
) : CaseInstanceLifecycleListener {
    companion object {
        private val LOG = Logger.getLogger(EndCaseLifecycleListener::class.java.getName())
        private const val EINDSTATUS_TOELICHTING = "Zaak beeindigd"
    }

    override fun getSourceState() = sourceState

    override fun getTargetState() = targetState

    override fun stateChanged(caseInstance: CaseInstance, oldState: String, newState: String) {
        val zaakUUID = UUID.fromString(caseInstance.businessKey)
        if (FlowableHelper.getInstance().zrcClientService.readZaak(zaakUUID).isOpen) {
            LOG.info("Zaak %${caseInstance.businessKey}: End Zaak")
            FlowableHelper.getInstance().zgwApiService.endZaak(zaakUUID, EINDSTATUS_TOELICHTING)
        }
    }
}
