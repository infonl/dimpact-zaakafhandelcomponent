/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak
import nl.info.client.smartdocuments.model.document.AanvragerData
import nl.info.client.smartdocuments.model.document.Data
import nl.info.client.smartdocuments.model.document.GebruikerData
import nl.info.client.smartdocuments.model.document.StartformulierData
import nl.info.client.smartdocuments.model.document.TaskData
import nl.info.client.smartdocuments.model.document.ZaakData
import nl.info.client.zgw.model.createZaak
import java.net.URI
import java.time.ZonedDateTime

fun createAanvragerData(
    naam: String = "fakeNaam",
    straat: String = "fakeStraat",
    huisnummer: String = "fakeHuisnummer",
    postcode: String = "fakePostcode",
    woonplaats: String = "fakeWoonplaats",
) = AanvragerData(
    naam = naam,
    straat = straat,
    huisnummer = huisnummer,
    postcode = postcode,
    woonplaats = woonplaats
)

fun createData(
    startformulier: StartformulierData = createStartformulierData(),
    zaakData: ZaakData = createZaakData(),
    taskData: TaskData = createTaskData(),
    gebruikerData: GebruikerData = createGebruikerData(),
    aanvragerData: AanvragerData = createAanvragerData(),
) = Data(
    startformulierData = startformulier,
    zaakData = zaakData,
    taskData = taskData,
    gebruikerData = gebruikerData,
    aanvragerData = aanvragerData
)

@Suppress("LongParameterList")
fun createDocumentCreationDataAttended(
    zaak: Zaak = createZaak(),
    taskId: String = "fakeTaskId",
    templateGroupId: String = "1",
    templateId: String = "2",
    title: String = "title",
    creationDate: ZonedDateTime = ZonedDateTime.now(),
) = DocumentCreationDataAttended(
    zaak = zaak,
    taskId = taskId,
    templateGroupId = templateGroupId,
    templateId = templateId,
    title = title,
    creationDate = creationDate
)

fun createDocumentCreationAttendedResponse(
    message: String = "success",
    redirectUri: URI = URI.create("https://example.com/fakeRedirectURI")
) = DocumentCreationAttendedResponse(
    message = message,
    redirectUrl = redirectUri
)

fun createGebruikerData(
    id: String = "fakeId",
    naam: String = "fakeNaam"
) = GebruikerData(
    id = id,
    naam = naam
)

fun createStartformulierData(
    productAanvraagtype: String = "fakeProductAanvraagtype",
    data: Map<String, Any> = mapOf("fakeKey" to "fakeValue")
) = StartformulierData(
    productAanvraagtype = productAanvraagtype,
    data = data
)

fun createTaskData(
    naam: String = "fakeNaam",
    behandelaar: String = "fakeBehandelaar"
) = TaskData(
    naam = naam,
    behandelaar = behandelaar
)

fun createZaakData(
    zaaktype: String = "fakeZaakType",
    identificatie: String = "fakeIdentificatie",
    omschrijving: String = "fakeOmschrijving",
    toelichting: String = "fakeToelichting",
) = ZaakData(
    zaaktype = zaaktype,
    identificatie = identificatie,
    omschrijving = omschrijving,
    toelichting = toelichting
)
