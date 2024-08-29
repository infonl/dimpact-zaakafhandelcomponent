/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.zac.app.zaak.model.RestOpenstaandeTaken
import net.atos.zac.flowable.task.FlowableTaskService
import java.util.UUID

class RestOpenstaandeTakenConverter @Inject constructor(
    private val flowableTaskService: FlowableTaskService
) {
    fun convert(zaakUUID: UUID): RestOpenstaandeTaken =
        flowableTaskService.listOpenTasksForZaak(zaakUUID).let { openTasks ->
            RestOpenstaandeTaken(
                aantalOpenstaandeTaken = openTasks.size,
                taakNamen = openTasks.map { it.name }
            )
        }
}
