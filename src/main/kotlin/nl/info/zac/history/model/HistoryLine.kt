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
    val attribuutLabel: String,
    @get:JsonbProperty("oudeWaarde") val oldValue: String?,
    @get:JsonbProperty("nieuweWaarde") val newValue: String?
) {
    var datumTijd: ZonedDateTime? = null
    var door: String? = null
    var applicatie: String? = null
    var toelichting: String? = null
    var actie: HistoryAction? = null

    constructor(attribuutLabel: String, oldValue: LocalDate?, newValue: LocalDate?) : this(
        attribuutLabel,
        oldValue?.toValue(),
        newValue?.toValue()
    )

    constructor(attribuutLabel: String, oldValue: ZonedDateTime?, newValue: ZonedDateTime?) : this(
        attribuutLabel,
        oldValue?.toValue(),
        newValue?.toValue()
    )

    constructor(attribuutLabel: String, oldValue: Boolean?, newValue: Boolean?) : this(
        attribuutLabel,
        oldValue?.toValue(),
        newValue?.toValue()
    )

    constructor(
        attribuutLabel: String,
        oldValue: StatusEnum?,
        newValue: StatusEnum?
    ) : this(attribuutLabel, oldValue?.toValue(), newValue?.toValue())

    constructor(
        attribuutLabel: String,
        oldValue: VertrouwelijkheidaanduidingEnum?,
        newValue: VertrouwelijkheidaanduidingEnum?
    ) : this(attribuutLabel, oldValue?.toValue(), newValue?.toValue())
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
