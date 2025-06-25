/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.delegate.JavaDelegate
import java.util.logging.Logger

class BerichtenboxDelegate : JavaDelegate {
    private var verzendenGelukt = true

    companion object {
        private val LOG: Logger = Logger.getLogger(BerichtenboxDelegate::class.java.name)

        private const val BERICHTENBOX_VERZONDEN_VARIABLE = "berichtenbox_verzonden"
        private const val VERZENDEN_METHODE_VARIABLE = "verzend_methode"
        private const val VERZENDEN_METHODE_POST = "post"
    }

    override fun execute(delegateExecution: DelegateExecution) {
        LOG.info("Verstuur besluit via Berichtenbox.")
        LOG.info("VerzendenGelukt = $verzendenGelukt")
        delegateExecution.setVariable(BERICHTENBOX_VERZONDEN_VARIABLE, verzendenGelukt)
        if (!verzendenGelukt) {
            delegateExecution.setVariable(VERZENDEN_METHODE_VARIABLE, VERZENDEN_METHODE_POST)
        }
        verzendenGelukt = !verzendenGelukt
    }
}
