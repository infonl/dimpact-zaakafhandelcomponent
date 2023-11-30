/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken.model

import java.util.UUID

fun createRESTTaakToekennenGegevens(
    taakId: String = "dummyTaakId",
    zaakUuid: UUID = UUID.randomUUID(),
    groepId: String = "dummyGroepId",
    behandelaarId: String = "dummyBehandelaarId",
    reden: String = "dummyReden"
) = RESTTaakToekennenGegevens().apply {
    this.taakId = taakId
    this.zaakUuid = zaakUuid
    this.groepId = groepId
    this.behandelaarId = behandelaarId
    this.reden = reden
}
