/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.zgw.zrc.util.isOpen
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
        if (FlowableHelper.getInstance().zrcClientService.readZaak(zaakUUID).isOpen()) {
            LOG.info("Zaak %${caseInstance.businessKey}: End Zaak")
            try {
                FlowableHelper.getInstance().zgwApiService.endZaak(zaakUUID, "TODO resultaattypeomschrijving", EINDSTATUS_TOELICHTING)
            } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                // rethrow as an FlowableException, just to ensure that it is logged in [CommandContext] at log level INFO instead of ERROR
                throw FlowableZgwValidationErrorException("Failed to end zaak", zgwValidationErrorException)
            }
        }
    }
}
