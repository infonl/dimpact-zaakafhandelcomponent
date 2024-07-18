/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter

import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.app.audit.model.RESTHistorieRegel

abstract class AbstractAuditWijzigingConverter<W : AuditWijziging<*>?> {
    fun convert(wijziging: AuditWijziging<*>): List<RESTHistorieRegel> =
        doConvert(wijziging as W)

    abstract fun supports(objectType: ObjectType): Boolean

    protected abstract fun doConvert(wijziging: W): List<RESTHistorieRegel>
}
