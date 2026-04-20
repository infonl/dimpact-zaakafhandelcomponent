/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.zgw.util.extractUuid
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.logging.Logger

/**
 * Flowable BPMN delegate to update a zaak.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks created with (previous versions of) the related BPMN process.
 */
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

        val resultaattypeDescription = resultaattypeOmschrijving?.resolveValueAsString(execution)
        if (resultaattypeDescription != null) {
            LOG.fine(
                "Closing zaak with UUID '${zaak.getUuid()}' using resultaattype omschrijving '$resultaattypeDescription'"
            )
            try {
                val resultaattype = flowableHelper.zgwApiService.getResultaatType(
                    zaak.zaaktype,
                    resultaattypeDescription
                )
                flowableHelper.zgwApiService.closeZaak(zaak, resultaattype.url.extractUuid(), TOELICHTING)
            } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                // rethrow as a FlowableException
                // just to ensure that it is logged in [CommandContext] at log level INFO instead of ERROR
                throw FlowableZgwValidationErrorException("Failed to close zaak", zgwValidationErrorException)
            }
        } else {
            val statustypeOmschrijving = statustypeOmschrijving.resolveValueAsString(execution)
            LOG.fine(
                "Creating status for zaak with UUID '${zaak.getUuid()}' using statustype description: '$statustypeOmschrijving'"
            )
            flowableHelper.zgwApiService.createStatusForZaak(zaak, statustypeOmschrijving, TOELICHTING)
        }
    }
}
