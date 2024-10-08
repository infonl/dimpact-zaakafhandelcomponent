/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation.model

import net.atos.client.smartdocuments.model.document.AanvragerData
import net.atos.client.smartdocuments.model.document.Data
import net.atos.client.smartdocuments.model.document.GebruikerData
import net.atos.client.smartdocuments.model.document.StartformulierData
import net.atos.client.smartdocuments.model.document.TaakData
import net.atos.client.smartdocuments.model.document.ZaakData
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaak
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
    taakData: TaakData = createTaakData(),
    gebruikerData: GebruikerData = createGebruikerData(),
    aanvragerData: AanvragerData = createAanvragerData(),
) = Data(
    startformulierData = startformulier,
    zaakData = zaakData,
    taakData = taakData,
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

fun createTaakData(
    naam: String = "dummyNaam",
    behandelaar: String = "dummyBehandelaar",
    data: Map<String, String> = mapOf("dummyKey" to "dummyValue")
) = TaakData(
    naam = naam,
    behandelaar = behandelaar,
    data = data
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
