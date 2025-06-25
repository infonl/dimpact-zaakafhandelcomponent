/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.zac.flowable.ZaakVariabelenService
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.delegate.JavaDelegate

abstract class AbstractDelegate : JavaDelegate {
    protected fun getZaakIdentificatie(delegateExecution: DelegateExecution): String {
        return delegateExecution.parent.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) as String
    }
}
