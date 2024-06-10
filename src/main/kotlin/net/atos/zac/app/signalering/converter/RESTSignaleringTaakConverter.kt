package net.atos.zac.app.signaleringen.converter

import jakarta.inject.Inject
import net.atos.zac.app.signaleringen.model.RESTSignaleringTaakSummary
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.util.DateTimeConverterUtil
import org.flowable.task.api.TaskInfo

class RESTSignaleringTaakConverter @Inject constructor(
    private var taakVariabelenService: TaakVariabelenService
) {
    fun convert(taskInfo: TaskInfo): RESTSignaleringTaakSummary =
        RESTSignaleringTaakSummary(
            taskInfo.id,
            taskInfo.name,
            taakVariabelenService.readZaakIdentificatie(taskInfo),
            taakVariabelenService.readZaaktypeOmschrijving(taskInfo),
            creatiedatumTijd = DateTimeConverterUtil.convertToZonedDateTime(taskInfo.createTime)
        )
}
