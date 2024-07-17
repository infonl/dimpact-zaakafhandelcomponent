/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter

import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.app.audit.model.RESTHistorieRegel
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.stream.Stream

abstract class AbstractAuditWijzigingConverter<W : AuditWijziging<*>?> {
    fun convert(wijziging: AuditWijziging<*>): Stream<RESTHistorieRegel> {
        return doConvert(wijziging as W)
    }

    abstract fun supports(objectType: ObjectType): Boolean

    protected abstract fun doConvert(wijziging: W): Stream<RESTHistorieRegel>

    protected fun checkAttribuut(
        label: String,
        oud: String,
        nieuw: String,
        historieRegels: MutableList<RESTHistorieRegel>
    ) {
        if (!StringUtils.equals(oud, nieuw)) {
            historieRegels.add(RESTHistorieRegel(label, oud, nieuw))
        }
    }

    protected fun checkAttribuut(
        label: String,
        oud: StatusEnum,
        nieuw: StatusEnum,
        historieRegels: MutableList<RESTHistorieRegel>
    ) {
        if (oud != nieuw) {
            historieRegels.add(RESTHistorieRegel(label, oud, nieuw))
        }
    }

    protected fun checkAttribuut(
        label: String,
        oud: VertrouwelijkheidaanduidingEnum,
        nieuw: VertrouwelijkheidaanduidingEnum,
        historieRegels: MutableList<RESTHistorieRegel>
    ) {
        if (oud != nieuw) {
            historieRegels.add(RESTHistorieRegel(label, oud, nieuw))
        }
    }

    protected fun checkAttribuut(
        label: String,
        oud: Boolean,
        nieuw: Boolean,
        historieRegels: MutableList<RESTHistorieRegel>
    ) {
        if (ObjectUtils.notEqual(oud, nieuw)) {
            historieRegels.add(RESTHistorieRegel(label, oud, nieuw))
        }
    }

    protected fun checkAttribuut(
        label: String,
        oud: LocalDate?,
        nieuw: LocalDate?,
        historieRegels: MutableList<RESTHistorieRegel>
    ) {
        if (ObjectUtils.notEqual(oud, nieuw)) {
            historieRegels.add(RESTHistorieRegel(label, oud, nieuw))
        }
    }

    protected fun checkAttribuut(
        label: String,
        oud: ZonedDateTime,
        nieuw: ZonedDateTime,
        historieRegels: MutableList<RESTHistorieRegel>
    ) {
        if (ObjectUtils.notEqual(oud, nieuw)) {
            historieRegels.add(RESTHistorieRegel(label, oud, nieuw))
        }
    }
}
