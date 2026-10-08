/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

/**
 * The data with which the process of a new zaak starts. Only BPMN processes receive the assignment of the zaak as
 * process variables, because only BPMN process definitions read it.
 */
data class ProcessStartData(
    val zaakData: Map<String, Any> = emptyMap(),
    val groupId: String? = null,
    val behandelaarId: String? = null,
    val communicatiekanaal: String? = null
)
