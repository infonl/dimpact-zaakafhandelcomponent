/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.task.model

import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.informatieobjecten.model.RestInformatieobjecttype
import net.atos.zac.app.informatieobjecten.model.createRestInformatieobjecttype
import net.atos.zac.app.zaak.model.createRestUser
import java.util.UUID

@Suppress("LongParameterList")
fun createRestTask(
    id: String = "dummyId",
    behandelaar: RestUser = createRestUser(),
    taakData: MutableMap<String, Any> = emptyMap<String, Any>().toMutableMap(),
    tabellen: MutableMap<String, List<String>> = emptyMap<String, List<String>>().toMutableMap(),
    zaakIdentificatie: String = "dummyZaakIndentificatie",
    zaakUuid: UUID = UUID.randomUUID()
) = RestTask(
    id = id,
    behandelaar = behandelaar,
    taakdata = taakData,
    tabellen = tabellen,
    zaakIdentificatie = zaakIdentificatie,
    zaakUuid = zaakUuid
)

fun createRestTaskDocumentData(
    bestandsnaam: String = "dummyBestandsNaam",
    documentTitel: String = "dummyDocumentTitel",
    documentType: RestInformatieobjecttype = createRestInformatieobjecttype()
) = RestTaskDocumentData(
    bestandsnaam = bestandsnaam,
    documentTitel = documentTitel,
    documentType = documentType
)

fun createRestTaskAssignData(
    taakId: String = "dummyTaakId",
    zaakUuid: UUID = UUID.randomUUID(),
    groepId: String = "dummyGroepId",
    behandelaarId: String? = "dummyBehandelaarId",
    reden: String = "dummyReden"
) = RestTaskAssignData(
    taakId = taakId,
    zaakUuid = zaakUuid,
    groepId = groepId,
    behandelaarId = behandelaarId,
    reden = reden
)

fun createRestTaskDistributeTask(
    taakId: String = "dummyTaakId",
    zaakUuid: UUID = UUID.randomUUID()
) = RestTaskDistributeTask(
    taakId = taakId,
    zaakUuid = zaakUuid
)

fun createRestTaskDistributeData(
    taken: List<RestTaskDistributeTask> = listOf(createRestTaskDistributeTask()),
    groepId: String = "dummyGroepId",
    behandelaarGebruikersnaam: String? = "dummyBehandelaarGebruikersnaam",
    reden: String = "dummyReason",
    screenEventResourceId: String? = "dummyScreenEventResourceId"
) = RestTaskDistributeData(
    taken = taken,
    groepId = groepId,
    behandelaarGebruikersnaam = behandelaarGebruikersnaam,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)

fun createRestTaskReleaseData(
    taken: List<RestTaskDistributeTask> = listOf(createRestTaskDistributeTask()),
    reden: String = "dummyReason",
    screenEventResourceId: String? = "dummyScreenEventResourceId"
) = RestTaskReleaseData(
    taken = taken,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)
