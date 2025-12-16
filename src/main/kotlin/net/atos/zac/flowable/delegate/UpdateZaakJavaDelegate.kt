/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
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

        val resultaattypeOmschrijving = resultaattypeOmschrijving?.resolveValueAsString(execution)
        if (resultaattypeOmschrijving != null) {
            LOG.info(
                "Zaak '${zaak.getUuid()}': Closing zaak with resultaattype omschrijving " +
                    "'$resultaattypeOmschrijving'"
            )
            try {
                flowableHelper.zgwApiService.endZaak(zaak, resultaattypeOmschrijving, TOELICHTING)
            } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                // rethrow as a FlowableException
                // just to ensure that it is logged in [CommandContext] at log level INFO instead of ERROR
                throw FlowableZgwValidationErrorException("Failed to end zaak", zgwValidationErrorException)
            }
            return
        }

        val statustypeOmschrijving = statustypeOmschrijving.resolveValueAsString(execution)
        // For non-end statuses, use the regular createStatusForZaak endpoint
        LOG.fine("Zaak '${zaak.getUuid()}': setting statustype '$statustypeOmschrijving'")
        flowableHelper.zgwApiService.createStatusForZaak(zaak, statustypeOmschrijving, TOELICHTING)
    }
}
