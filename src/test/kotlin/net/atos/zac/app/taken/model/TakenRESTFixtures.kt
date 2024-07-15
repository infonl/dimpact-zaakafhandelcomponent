/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken.model

import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjecttype
import net.atos.zac.app.informatieobjecten.model.createRESTInformatieobjecttype
import net.atos.zac.app.zaak.model.createRESTUser
import java.util.UUID

fun createRESTTaak(
    id: String = "dummyId",
    zaakUuid: UUID = UUID.randomUUID(),
    behandelaar: RESTUser = createRESTUser(),
    taakData: MutableMap<String, String> = emptyMap<String, String>().toMutableMap(),
    tabellen: MutableMap<String, List<String>> = emptyMap<String, List<String>>().toMutableMap()
) = RESTTaak(
    id = id,
    zaakUuid = zaakUuid,
    behandelaar = behandelaar,
    taakdata = taakData,
    tabellen = tabellen
)

fun createRESTTaakDocumentData(
    bestandsnaam: String = "dummyBestandsNaam",
    documentTitel: String = "dummyDocumentTitel",
    documentType: RESTInformatieobjecttype = createRESTInformatieobjecttype()
) = RESTTaakDocumentData(
    bestandsnaam = bestandsnaam,
    documentTitel = documentTitel,
    documentType = documentType
)

fun createRESTTaakToekennenGegevens(
    taakId: String = "dummyTaakId",
    zaakUuid: UUID = UUID.randomUUID(),
    groepId: String = "dummyGroepId",
    behandelaarId: String = "dummyBehandelaarId",
    reden: String = "dummyReden"
) = RESTTaakToekennenGegevens(
    taakId = taakId,
    zaakUuid = zaakUuid,
    groepId = groepId,
    behandelaarId = behandelaarId,
    reden = reden
)

fun createRESTTaakVerdelenTaak(
    taakId: String = "dummyTaakId",
    zaakUuid: UUID = UUID.randomUUID()
) = RESTTaakVerdelenTaak(
    taakId = taakId,
    zaakUuid = zaakUuid
)

fun createRESTTaakVerdelenGegevens(
    taken: List<RESTTaakVerdelenTaak> = listOf(createRESTTaakVerdelenTaak()),
    groepId: String = "dummyGroepId",
    behandelaarGebruikersnaam: String? = "dummyBehandelaarGebruikersnaam",
    reden: String = "dummyReason",
    screenEventResourceId: String? = "dummyScreenEventResourceId"
) = RESTTaakVerdelenGegevens(
    taken = taken,
    groepId = groepId,
    behandelaarGebruikersnaam = behandelaarGebruikersnaam,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)

fun createRESTTaakVrijgevenGegevens(
    taken: List<RESTTaakVerdelenTaak> = listOf(createRESTTaakVerdelenTaak()),
    reden: String = "dummyReason",
    screenEventResourceId: String? = "dummyScreenEventResourceId"
) = RESTTaakVrijgevenGegevens(
    taken = taken,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)
