/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import org.apache.commons.lang3.BooleanUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HistoryLine(
    @get:JsonbProperty("attribuutLabel") val attributeLabel: String,
    @get:JsonbProperty("oudeWaarde") val oldValue: String?,
    @get:JsonbProperty("nieuweWaarde") val newValue: String?
) {
    @get:JsonbProperty("datumTijd")
    var zonedDateTime: ZonedDateTime? = null

    @get:JsonbProperty("door")
    var by: String? = null

    @get:JsonbProperty("applicatie")
    var application: String? = null

    @get:JsonbProperty("toelichting")
    var explanation: String? = null

    @get:JsonbProperty("actie")
    var action: HistoryAction? = null

    constructor(attributeLabel: String, oldValue: LocalDate?, newValue: LocalDate?) : this(
        attributeLabel,
        oldValue?.toValue(),
        newValue?.toValue()
    )

    constructor(attributeLabel: String, oldValue: ZonedDateTime?, newValue: ZonedDateTime?) : this(
        attributeLabel,
        oldValue?.toValue(),
        newValue?.toValue()
    )

    constructor(attributeLabel: String, oldValue: Boolean?, newValue: Boolean?) : this(
        attributeLabel,
        oldValue?.toValue(),
        newValue?.toValue()
    )

    constructor(
        attributeLabel: String,
        oldValue: StatusEnum?,
        newValue: StatusEnum?
    ) : this(attributeLabel, oldValue?.toValue(), newValue?.toValue())

    constructor(
        attributeLabel: String,
        oldValue: VertrouwelijkheidaanduidingEnum?,
        newValue: VertrouwelijkheidaanduidingEnum?
    ) : this(attributeLabel, oldValue?.toValue(), newValue?.toValue())
}

fun LocalDate.toValue(): String = DATE_FORMATTER.format(this)

fun ZonedDateTime.toValue(): String = DATE_TIME_FORMATTER.format(this)

fun StatusEnum.toValue(): String = toString()

fun VertrouwelijkheidaanduidingEnum.toValue(): String = toString()

fun Boolean.toValue(): String = BooleanUtils.toString(this, TRUE, FALSE)

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    .withZone(ZoneId.systemDefault())

private const val TRUE = "Ja"

private const val FALSE = "Nee"
