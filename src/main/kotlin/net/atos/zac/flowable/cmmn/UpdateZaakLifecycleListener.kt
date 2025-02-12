/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.zac.flowable.FlowableHelper
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener
import org.flowable.cmmn.api.runtime.PlanItemInstance
import org.flowable.common.engine.api.delegate.Expression
import java.util.logging.Logger


/**
 * Custom Flowable update zaak lifecycle listener.
 *
 * This class is used in our [CMMN model](../../../../resources/cmmn/Generiek_zaakafhandelmodel.cmmn.xml)
 * so be careful renaming or moving it to another package because that will break all zaken and tasks
 * that were created with previous versions of our CMMN model.
 */
@NoArgConstructor
class UpdateZaakLifecycleListener : PlanItemInstanceLifecycleListener {
    companion object {
        private val LOG = Logger.getLogger(UpdateZaakLifecycleListener::class.java.getName())
        private const val STATUS_TOELICHTING = "Status gewijzigd"
    }

    private var statusExpression: Expression? = null

    fun setStatus(status: Expression) {
        statusExpression = status
    }

    override fun getSourceState() = null

    override fun getTargetState() = null

    override fun stateChanged(
        planItemInstance: DelegatePlanItemInstance,
        oldState: String,
        newState: String?
    ) {
        statusExpression?.let {
            updateZaak(planItemInstance, it.getValue(planItemInstance).toString())
        }
    }

    private fun updateZaak(planItemInstance: PlanItemInstance, statustypeOmschrijving: String) {
        val zaakUUID = FlowableHelper.getInstance().zaakVariabelenService.readZaakUUID(planItemInstance)
        val zaak = FlowableHelper.getInstance().zrcClientService.readZaak(zaakUUID)
        LOG.info("Zaak '$zaakUUID': Change Status to '$statustypeOmschrijving'")
        FlowableHelper.getInstance().zgwApiService.createStatusForZaak(zaak, statustypeOmschrijving, STATUS_TOELICHTING)
    }
}
