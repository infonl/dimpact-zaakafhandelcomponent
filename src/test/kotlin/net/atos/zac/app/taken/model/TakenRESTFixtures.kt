/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken.model

import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjecttype
import net.atos.zac.app.informatieobjecten.model.createRESTInformatieobjecttype
import net.atos.zac.app.zaken.model.createRESTUser
import java.util.UUID

fun createRESTTaak(
    id: String = "dummyId",
    zaakUuid: UUID = UUID.randomUUID(),
    behandelaar: RESTUser = createRESTUser(),
    taakData: Map<String, String> = emptyMap()
) = RESTTaak().apply {
    this.id = id
    this.zaakUuid = zaakUuid
    this.behandelaar = behandelaar
    this.taakdata = taakData
}

fun createRESTTaakDocumentData(
    bestandsnaam: String = "dummyBestandsNaam",
    documentTitel: String = "dummyDocumentTitel",
    documentType: RESTInformatieobjecttype = createRESTInformatieobjecttype()
) = RESTTaakDocumentData().apply {
    this.bestandsnaam = bestandsnaam
    this.documentTitel = documentTitel
    this.documentType = documentType
}

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
