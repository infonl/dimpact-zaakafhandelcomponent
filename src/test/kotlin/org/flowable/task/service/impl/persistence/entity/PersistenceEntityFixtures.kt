package org.flowable.task.service.impl.persistence.entity

import org.flowable.task.api.history.HistoricTaskLogEntryType

fun createHistoricTaskLogEntryEntityImpl(
    type: HistoricTaskLogEntryType = HistoricTaskLogEntryType.USER_TASK_CREATED,
    data: String = "{\"dummyKey\":\"dummyValue\"}"
) = HistoricTaskLogEntryEntityImpl().apply {
    this.type = type.toString()
    this.data = data
}
