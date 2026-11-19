/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import nl.info.zac.time.ZacTime
import java.time.ZonedDateTime

fun createBpmnProcessDefinitionMetadata(
    documentation: String? = "Test Documentation",
    modificationDate: ZonedDateTime? = ZacTime.now(),
    uploadDate: ZonedDateTime? = ZacTime.now(),
    formKeys: List<String> = emptyList()
) = BpmnProcessDefinitionMetadata(
    documentation = documentation,
    modificationDate = modificationDate,
    uploadDate = uploadDate,
    formKeys = formKeys
)
