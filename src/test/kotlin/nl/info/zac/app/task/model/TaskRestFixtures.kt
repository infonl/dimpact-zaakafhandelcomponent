/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.task.model

import jakarta.json.JsonObject
import net.atos.zac.app.informatieobjecten.model.RestInformatieobjecttype
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.informatieobjecten.model.createRestInformatieobjecttype
import nl.info.zac.app.policy.model.createRestTaakRechten
import nl.info.zac.app.zaak.model.createRestUser
import java.util.UUID

@Suppress("LongParameterList")
fun createRestTask(
    id: String = "fakeId",
    behandelaar: RestUser = createRestUser(),
    taakData: MutableMap<String, Any> = emptyMap<String, Any>().toMutableMap(),
    tabellen: MutableMap<String, List<String>> = emptyMap<String, List<String>>().toMutableMap(),
    zaakIdentificatie: String = "fakeZaakIndentificatie",
    zaakUuid: UUID = UUID.randomUUID(),
    formioFormulier: JsonObject? = null
) = RestTask(
    id = id,
    behandelaar = behandelaar,
    taakdata = taakData,
    tabellen = tabellen,
    zaakIdentificatie = zaakIdentificatie,
    zaakUuid = zaakUuid,
    formioFormulier = formioFormulier,
    rechten = createRestTaakRechten(),
)

fun createRestTaskDocumentData(
    bestandsnaam: String = "fakeBestandsNaam",
    documentTitel: String = "fakeDocumentTitel",
    documentType: RestInformatieobjecttype = createRestInformatieobjecttype()
) = RestTaskDocumentData(
    bestandsnaam = bestandsnaam,
    documentTitel = documentTitel,
    documentType = documentType
)

fun createRestTaskAssignData(
    taakId: String = "fakeTaakId",
    zaakUuid: UUID = UUID.randomUUID(),
    groepId: String = "fakeGroepId",
    behandelaarId: String? = "fakeBehandelaarId",
    reden: String = "fakeReden"
) = RestTaskAssignData(
    taakId = taakId,
    zaakUuid = zaakUuid,
    groepId = groepId,
    behandelaarId = behandelaarId,
    reden = reden
)

fun createRestTaskDistributeTask(
    taakId: String = "fakeTaakId",
    zaakUuid: UUID = UUID.randomUUID()
) = RestTaskDistributeTask(
    taakId = taakId,
    zaakUuid = zaakUuid
)

fun createRestTaskDistributeData(
    taken: List<RestTaskDistributeTask> = listOf(createRestTaskDistributeTask()),
    groepId: String = "fakeGroepId",
    behandelaarGebruikersnaam: String? = "fakeBehandelaarGebruikersnaam",
    reden: String = "fakeReason",
    screenEventResourceId: String? = "fakeScreenEventResourceId"
) = RestTaskDistributeData(
    taken = taken,
    groepId = groepId,
    behandelaarGebruikersnaam = behandelaarGebruikersnaam,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)

fun createRestTaskReleaseData(
    taken: List<RestTaskDistributeTask> = listOf(createRestTaskDistributeTask()),
    reden: String = "fakeReason",
    screenEventResourceId: String? = "fakeScreenEventResourceId"
) = RestTaskReleaseData(
    taken = taken,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)
