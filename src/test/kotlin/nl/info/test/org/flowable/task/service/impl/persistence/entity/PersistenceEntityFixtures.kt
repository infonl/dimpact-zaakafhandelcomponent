/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.test.org.flowable.task.service.impl.persistence.entity

import org.flowable.task.api.history.HistoricTaskLogEntryType
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl

fun createHistoricTaskInstanceEntityImpl() = HistoricTaskInstanceEntityImpl()

fun createHistoricTaskLogEntryEntityImpl(
    type: HistoricTaskLogEntryType = HistoricTaskLogEntryType.USER_TASK_CREATED,
    data: String = "{\"dummyKey\":\"dummyValue\"}"
) = HistoricTaskLogEntryEntityImpl().apply {
    this.type = type.toString()
    this.data = data
}
