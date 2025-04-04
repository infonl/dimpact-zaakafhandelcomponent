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
    naam: String = "dummyNaam",
    straat: String = "dummyStraat",
    huisnummer: String = "dummyHuisnummer",
    postcode: String = "dummyPostcode",
    woonplaats: String = "dummyWoonplaats",
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
    taskId: String = "dummyTaskId",
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
    redirectUri: URI = URI.create("https://example.com/dummyRedirectURI")
) = DocumentCreationAttendedResponse(
    message = message,
    redirectUrl = redirectUri
)

fun createGebruikerData(
    id: String = "dummyId",
    naam: String = "dummyNaam"
) = GebruikerData(
    id = id,
    naam = naam
)

fun createStartformulierData(
    productAanvraagtype: String = "dummyProductAanvraagtype",
    data: Map<String, Any> = mapOf("dummyKey" to "dummyValue")
) = StartformulierData(
    productAanvraagtype = productAanvraagtype,
    data = data
)

fun createTaskData(
    naam: String = "dummyNaam",
    behandelaar: String = "dummyBehandelaar"
) = TaskData(
    naam = naam,
    behandelaar = behandelaar
)

fun createZaakData(
    zaaktype: String = "dummyZaakType",
    identificatie: String = "dummyIdentificatie",
    omschrijving: String = "dummyOmschrijving",
    toelichting: String = "dummyToelichting",
) = ZaakData(
    zaaktype = zaaktype,
    identificatie = identificatie,
    omschrijving = omschrijving,
    toelichting = toelichting
)
