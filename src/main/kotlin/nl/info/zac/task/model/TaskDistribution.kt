/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task.model

/**
 * The tasks to assign to a groep and, optionally, to a behandelaar.
 *
 * @property username the behandelaar to assign the tasks to, or null to release the tasks from their behandelaar
 */
data class TaskDistribution(
    val taskIds: List<String>,
    val groupId: String,
    val username: String? = null,
    val reason: String? = null
)
