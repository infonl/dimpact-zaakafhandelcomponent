/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.zac.app.zaken.model.RESTOpenstaandeTaken
import net.atos.zac.flowable.FlowableTaskService
import java.util.UUID

class RESTOpenstaandeTakenConverter {
    @Inject
    private lateinit var flowableTaskService: FlowableTaskService

    fun convert(zaakUUID: UUID): RESTOpenstaandeTaken {
        val openstaandeTaken = flowableTaskService.listOpenTasksForZaak(zaakUUID)
        return RESTOpenstaandeTaken(
            aantalOpenstaandeTaken = openstaandeTaken.size,
            taakNamen = openstaandeTaken.stream().map { it.name }.toList()
        )
    }
}
