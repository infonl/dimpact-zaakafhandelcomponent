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
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import java.net.URI

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

fun createDocumentCreationData(
    zaak: Zaak = createZaak(),
    taskId: String = "dummyTaskId",
    informatieobjecttype: InformatieObjectType = createInformatieObjectType()
) = DocumentCreationData(
    zaak = zaak,
    taskId = taskId,
    informatieobjecttype = informatieobjecttype
)

fun createDocumentCreationResponse(
    redirectUri: URI = URI.create("http://example.com/dummyRedirectURI")
) = DocumentCreationAttendedResponse(
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
