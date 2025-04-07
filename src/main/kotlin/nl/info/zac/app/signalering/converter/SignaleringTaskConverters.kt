/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.converter

import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.zac.app.signalering.model.RestSignaleringTaskSummary
import org.flowable.task.api.TaskInfo

fun TaskInfo.toRestSignaleringTaakSummary() =
    RestSignaleringTaskSummary(
        this.id,
        this.name,
        readZaakIdentificatie(this),
        readZaaktypeOmschrijving(this),
        creatiedatumTijd = DateTimeConverterUtil.convertToZonedDateTime(this.createTime)
    )
