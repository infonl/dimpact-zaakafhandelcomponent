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
    val resultaattypeOmschrijving: Expression? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(UpdateZaakJavaDelegate::class.java.name)

        private const val TOELICHTING = "Aangepast vanuit proces"
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))

        if (resultaattypeOmschrijving != null) {
            val resultaattypeOmschrijving = resultaattypeOmschrijving.resolveValueAsString(execution)
            LOG.info(
                "Zaak '${zaak.getUuid()}': Aanmaken Status met resultaattype omschrijving " +
                    "'$resultaattypeOmschrijving'"
            )
//            flowableHelper.zgwApiService.endZaak(zaak, resultaattypeOmschrijving, TOELICHTING)
        }

        val statustypeOmschrijving = statustypeOmschrijving.resolveValueAsString(execution)
        LOG.info("Zaak '${zaak.getUuid()}': Aanmaken Status met statustype omschrijving '$statustypeOmschrijving'")
        flowableHelper.zgwApiService.createStatusForZaak(zaak, statustypeOmschrijving, TOELICHTING)
    }
}
