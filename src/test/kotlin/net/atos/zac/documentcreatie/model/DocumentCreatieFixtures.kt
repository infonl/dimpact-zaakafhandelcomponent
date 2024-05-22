/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreatie.model

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
) = AanvragerData().apply {
    this.naam = naam
    this.straat = straat
    this.huisnummer = huisnummer
    this.postcode = postcode
    this.woonplaats = woonplaats
}

fun createData(
    startformulier: StartformulierData = createStartformulierData(),
    zaakData: ZaakData = createZaakData(),
    taakData: TaakData = createTaakData(),
    gebruikerData: GebruikerData = createGebruikerData(),
    aanvragerData: AanvragerData = createAanvragerData(),
) = Data().apply {
    this.startformulierData = startformulier
    this.zaakData = zaakData
    this.taakData = taakData
    this.gebruikerData = gebruikerData
    this.aanvragerData = aanvragerData
}

fun createDocumentCreatieGegevens(
    zaak: Zaak = createZaak(),
    taskId: String = "dummyTaskId",
    informatieobjecttype: InformatieObjectType = createInformatieObjectType()
) = DocumentCreatieGegevens(
    zaak,
    taskId,
    informatieobjecttype
)

fun createDocumentCreatieResponse(
    redirectURI: URI = URI.create("http://example.com/dummyRedirectURI")
) = DocumentCreatieResponse(
    redirectURI
)

fun createGebruikerData(
    id: String = "dummyId",
    naam: String = "dummyNaam"
) = GebruikerData().apply {
    this.id = id
    this.naam = naam
}

fun createStartformulierData(
    productAanvraagtype: String = "dummyProductAanvraagtype",
    data: Map<String, Any> = mapOf("dummyKey" to "dummyValue")
) = StartformulierData().apply {
    this.productAanvraagtype = productAanvraagtype
    this.data = data
}

fun createTaakData(
    naam: String = "dummyNaam",
    behandelaar: String = "dummyBehandelaar",
    data: Map<String, String> = mapOf("dummyKey" to "dummyValue")
) = TaakData().apply {
    this.naam = naam
    this.behandelaar = behandelaar
    this.data = data
}

fun createZaakData(
    zaaktype: String = "dummyZaakType",
    identificatie: String = "dummyIdentificatie",
    omschrijving: String = "dummyOmschrijving",
    toelichting: String = "dummyToelichting",
) = ZaakData().apply {
    this.zaaktype = zaaktype
    this.identificatie = identificatie
    this.omschrijving = omschrijving
    this.toelichting = toelichting
}
