/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task.model

fun createTaskDistribution(
    taskIds: List<String> = listOf("fakeTaskId"),
    groupId: String = "fakeGroepId",
    username: String? = "fakeBehandelaarGebruikersnaam",
    reason: String? = "fakeReason"
) = TaskDistribution(
    taskIds = taskIds,
    groupId = groupId,
    username = username,
    reason = reason
)
