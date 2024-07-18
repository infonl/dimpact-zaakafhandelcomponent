package net.atos.zac.app.audit.converter

import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.zac.app.audit.model.RESTHistorieRegel
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.ZonedDateTime

fun MutableList<RESTHistorieRegel>.addHistorieRegel(
    label: String,
    oud: String,
    nieuw: String
) {
    if (!StringUtils.equals(oud, nieuw)) {
        this.add(RESTHistorieRegel(label, oud, nieuw))
    }
}

fun MutableList<RESTHistorieRegel>.addHistorieRegel(
    label: String,
    oud: StatusEnum,
    nieuw: StatusEnum
) {
    if (oud != nieuw) {
        this.add(RESTHistorieRegel(label, oud, nieuw))
    }
}

fun MutableList<RESTHistorieRegel>.addHistorieRegel(
    label: String,
    oud: VertrouwelijkheidaanduidingEnum,
    nieuw: VertrouwelijkheidaanduidingEnum
) {
    if (oud != nieuw) {
        this.add(RESTHistorieRegel(label, oud, nieuw))
    }
}

fun MutableList<RESTHistorieRegel>.addHistorieRegel(
    label: String,
    oud: Boolean,
    nieuw: Boolean
) {
    if (ObjectUtils.notEqual(oud, nieuw)) {
        this.add(RESTHistorieRegel(label, oud, nieuw))
    }
}

fun MutableList<RESTHistorieRegel>.addHistorieRegel(
    label: String,
    oud: LocalDate?,
    nieuw: LocalDate?
) {
    if (ObjectUtils.notEqual(oud, nieuw)) {
        this.add(RESTHistorieRegel(label, oud, nieuw))
    }
}

fun MutableList<RESTHistorieRegel>.addHistorieRegel(
    label: String,
    oud: ZonedDateTime,
    nieuw: ZonedDateTime
) {
    if (ObjectUtils.notEqual(oud, nieuw)) {
        this.add(RESTHistorieRegel(label, oud, nieuw))
    }
}
