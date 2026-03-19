/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import java.time.ZonedDateTime

fun createBpmnProcessDefinitionMetadata(
    documentation: String? = "Test Documentation",
    modificationDate: ZonedDateTime? = ZonedDateTime.now(),
    uploadDate: ZonedDateTime? = ZonedDateTime.now(),
    formKeys: List<String> = emptyList()
) = BpmnProcessDefinitionMetadata(
    documentation = documentation,
    modificationDate = modificationDate,
    uploadDate = uploadDate,
    formKeys = formKeys
)
