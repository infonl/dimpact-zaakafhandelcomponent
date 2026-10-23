/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.converter

import nl.info.zac.app.signalering.model.RestSignaleringTaskSummary
import nl.info.zac.flowable.task.readZaakIdentificatie
import nl.info.zac.flowable.task.readZaaktypeOmschrijving
import nl.info.zac.util.time.convertToZonedDateTime
import org.flowable.task.api.TaskInfo

fun TaskInfo.toRestSignaleringTaakSummary(isZaakspecifiekGeautoriseerd: Boolean) =
    RestSignaleringTaskSummary(
        id = this.id,
        naam = this.name,
        zaakIdentificatie = readZaakIdentificatie(this),
        isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd,
        zaaktypeOmschrijving = readZaaktypeOmschrijving(this),
        creatiedatumTijd = this.createTime?.let(::convertToZonedDateTime)
            ?: error("Task '${this.id}' has no createTime")
    )
