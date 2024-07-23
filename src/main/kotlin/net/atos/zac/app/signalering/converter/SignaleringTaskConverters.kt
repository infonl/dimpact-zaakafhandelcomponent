package net.atos.zac.app.signalering.converter

import net.atos.zac.app.signalering.model.RestSignaleringTaskSummary
import net.atos.zac.flowable.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.util.DateTimeConverterUtil
import org.flowable.task.api.TaskInfo

fun TaskInfo.toRestSignaleringTaakSummary() =
    RestSignaleringTaskSummary(
        this.id,
        this.name,
        readZaakIdentificatie(this),
        readZaaktypeOmschrijving(this),
        creatiedatumTijd = DateTimeConverterUtil.convertToZonedDateTime(this.createTime)
    )
