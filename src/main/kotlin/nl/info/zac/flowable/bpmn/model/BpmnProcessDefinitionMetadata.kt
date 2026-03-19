/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import java.time.ZonedDateTime

data class BpmnProcessDefinitionMetadata(
    val documentation: String? = null,
    val modificationDate: ZonedDateTime? = null,
    val uploadDate: ZonedDateTime? = null,
    val formKeys: List<String> = emptyList(),
)
