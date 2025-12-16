/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

class UpdateZaakJavaDelegate : AbstractDelegate() {
    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var statustypeOmschrijving: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    var resultaattypeOmschrijving: Expression? = null

    companion object {
        private val LOG = Logger.getLogger(UpdateZaakJavaDelegate::class.java.name)

        private const val TOELICHTING = "Aangepast vanuit proces"
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        val statustypeOmschrijving = statustypeOmschrijving.resolveValueAsString(execution)
        
        // Check if the status is an end status
        val statustypes = flowableHelper.ztcClientService.readStatustypen(zaak.zaaktype)
        val statustype = statustypes.firstOrNull { statustypeOmschrijving == it.omschrijving }
        val isEindstatus = statustype?.isEindstatus == true

        // If there's a result and it's an end status, use endZaak() which properly closes the case
        val resultaattypeOmschrijvingValue = resultaattypeOmschrijving?.resolveValueAsString(execution)
        if (resultaattypeOmschrijvingValue != null && isEindstatus) {
            LOG.info(
                "Zaak '${zaak.getUuid()}': Closing zaak with resultaattype omschrijving " +
                    "'$resultaattypeOmschrijvingValue' and eindstatus '$statustypeOmschrijving'"
            )
            flowableHelper.zgwApiService.endZaak(zaak, resultaattypeOmschrijvingValue, TOELICHTING)
            return
        }

        // If it's an end status without a result, we cannot set it via createStatusForZaak
        // because that endpoint doesn't allow end statuses
        if (isEindstatus) {
            LOG.warning(
                "Zaak '${zaak.getUuid()}': Cannot set eindstatus '$statustypeOmschrijving' " +
                    "via createStatusForZaak endpoint. A resultaattype is required to close a zaak."
            )
            throw IllegalStateException(
                "Cannot set eindstatus '$statustypeOmschrijving' without a resultaattype. " +
                    "Use resultaattypeOmschrijving field in the BPMN service task to provide a resultaattype."
            )
        }

        // For non-end statuses, use the regular createStatusForZaak endpoint
        LOG.fine("Zaak '${zaak.getUuid()}': setting statustype '$statustypeOmschrijving'")
        flowableHelper.zgwApiService.createStatusForZaak(zaak, statustypeOmschrijving, TOELICHTING)
    }
}
