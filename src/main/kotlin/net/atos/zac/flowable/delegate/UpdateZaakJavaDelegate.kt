/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.cmmn.log.ZaakStatusChanging
import net.atos.zac.flowable.delegate.log.ZaakClosing
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.log.PolicyDenied
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

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
        private val LOG = ZacLogger.getLogger(UpdateZaakJavaDelegate::class)

        private const val TOELICHTING = "Aangepast vanuit proces"
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val (zaak, zaaktype) = flowableHelper.zaakService.readZaakAndZaakTypeByZaakID(getZaakIdentificatie(execution))
        val loggedInUser = flowableHelper.loggedInUserInstance.get()
        assertPolicy(
            flowableHelper.policyService.readZaakRechten(
                zaak = zaak,
                zaaktype = zaaktype,
                loggedInUser = loggedInUser
            ).canBehandelen,
            LOG,
            PolicyDenied(action = "updateZaak", zaakIdentificatie = zaak.identificatie)
        )

        val resultaattypeDescription = resultaattypeOmschrijving?.resolveValueAsString(execution)
        if (resultaattypeDescription != null) {
            LOG.fine(ZaakClosing(zaakUuid = zaak.uuid, resultaattypeOmschrijving = resultaattypeDescription))
            try {
                val resultaattype = flowableHelper.zgwApiService.getResultaatType(
                    zaak.zaaktype,
                    resultaattypeDescription
                )
                flowableHelper.zgwApiService.closeZaak(zaak, resultaattype.url.extractUuid(), TOELICHTING)
            } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                // rethrow as a FlowableException
                // just to ensure that it is logged in [CommandContext] at log level INFO instead of ERROR
                throw FlowableZgwValidationErrorException(
                    "Failed to close zaak with UUID: '${zaak.uuid}'",
                    zgwValidationErrorException
                )
            }
        } else {
            val statustypeOmschrijving = statustypeOmschrijving.resolveValueAsString(execution)
            LOG.fine(ZaakStatusChanging(zaakUuid = zaak.uuid, statustypeOmschrijving = statustypeOmschrijving))
            flowableHelper.zgwApiService.createStatusForZaak(zaak, statustypeOmschrijving, TOELICHTING)
        }
    }
}
