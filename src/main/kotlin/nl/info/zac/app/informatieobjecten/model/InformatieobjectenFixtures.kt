/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model

import net.atos.zac.app.informatieobjecten.model.RestDocumentVerzendGegevens
import java.time.LocalDate
import java.util.UUID

fun createRestDocumentVerzendGegevens(
    zaakUuid: UUID = UUID.randomUUID(),
    verzenddatum: LocalDate = LocalDate.now(),
    informatieobjecten: List<UUID> = listOf(UUID.randomUUID()),
    toelichting: String = "dummyToelichting",
) = RestDocumentVerzendGegevens().apply {
    this.zaakUuid = zaakUuid
    this.verzenddatum = verzenddatum
    this.informatieobjecten = informatieobjecten
    this.toelichting = toelichting
}
