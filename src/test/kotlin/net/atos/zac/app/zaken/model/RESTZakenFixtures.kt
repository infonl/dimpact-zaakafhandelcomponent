/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken.model

import net.atos.client.zgw.shared.model.Vertrouwelijkheidaanduiding
import net.atos.zac.app.admin.model.RESTZaakafhandelParameters
import net.atos.zac.app.bag.model.RESTOpenbareRuimte
import net.atos.zac.app.bag.model.RESTPand
import net.atos.zac.app.identity.model.RESTGroup
import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import net.atos.zac.app.policy.model.RESTZaakRechten
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import java.time.LocalDate
import java.util.UUID

// note: the value of the zaak type 'omschrijving' field is used to determine whether users
// are allowed access to a zaak type
const val ZAAK_TYPE_1_OMSCHRIJVING = "zaaktype1"
const val ZAAK_TYPE_2_OMSCHRIJVING = "zaaktype2"

fun createRESTBesluit() = RESTBesluit()

fun createRESTCommunicatiekanaal() = RESTCommunicatiekanaal()

fun createRESTGerelateerdeZaak() = RESTGerelateerdeZaak()

fun createRESTGeometry() = RESTGeometry()

fun createRESTGroup() = RESTGroup()

fun createRESTInboxProductaanvraag(
    id: Long = 1234L,
    productaanvraagObjectUUID: UUID = UUID.randomUUID(),
    aanvraagdocumentUUID: UUID = UUID.randomUUID()
) = RESTInboxProductaanvraag().apply {
    this.id = id
    this.productaanvraagObjectUUID = productaanvraagObjectUUID
    this.aanvraagdocumentUUID = aanvraagdocumentUUID
    aantalBijlagen = 0
    type = null
    ontvangstdatum = null
    initiatorID = null
}

fun createRESTOpenbareRuimte() = RESTOpenbareRuimte()

fun createRESTPand() = RESTPand()

fun createRESTUser() = RESTUser()

fun createRESTZaak() = RESTZaak().apply {
    uuid = UUID.randomUUID()
    identificatie = "ZA2023001"
    omschrijving = "Sample Zaak"
    toelichting = "This is a test zaak"
    zaaktype = createRESTZaaktype()
    status = createRESTZaakStatus()
    resultaat = createRESTZaakResultaat()
    besluiten = listOf(createRESTBesluit())
    bronorganisatie = "Sample Bronorganisatie"
    verantwoordelijkeOrganisatie = "Sample Verantwoordelijke Organisatie"
    registratiedatum = LocalDate.of(2023, 9, 14)
    startdatum = LocalDate.of(2023, 9, 15)
    einddatumGepland = LocalDate.of(2023, 10, 1)
    einddatum = LocalDate.of(2023, 10, 5)
    uiterlijkeEinddatumAfdoening = LocalDate.of(2023, 10, 10)
    publicatiedatum = LocalDate.of(2023, 9, 16)
    archiefActiedatum = LocalDate.of(2023, 10, 15)
    archiefNominatie = "Sample Archief Nominatie"
    communicatiekanaal = createRESTCommunicatiekanaal()
    vertrouwelijkheidaanduiding = "Sample Vertrouwelijkheidaanduiding"
    zaakgeometrie = createRESTGeometry()
    isOpgeschort = true
    redenOpschorting = "Sample Reden Opschorting"
    isVerlengd = true
    redenVerlenging = "Sample Reden Verlenging"
    duurVerlenging = "Sample Duur Verlenging"
    groep = createRESTGroup()
    behandelaar = createRESTUser()
    gerelateerdeZaken = listOf(createRESTGerelateerdeZaak())
    kenmerken = listOf(createRESTZaakKenmerk())
    eigenschappen = listOf(createRESTZaakEigenschap())
    zaakdata = createZaakData()
    initiatorIdentificatieType = IdentificatieType.BSN
    initiatorIdentificatie = "Sample Initiator Identificatie"
    isOpen = true
    isHeropend = false
    isHoofdzaak = true
    isDeelzaak = false
    isOntvangstbevestigingVerstuurd = true
    isBesluittypeAanwezig = false
    isInIntakeFase = true
    isProcesGestuurd = false
    rechten = createRESTZaakRechten()
}

fun createRESTZaakAanmaakGegevens() = RESTZaakAanmaakGegevens().apply {
    zaak = createRESTZaak()
    inboxProductaanvraag = createRESTInboxProductaanvraag()
    bagObjecten = listOf(createRESTPand(), createRESTOpenbareRuimte())
}

fun createRESTZaakEigenschap() = RESTZaakEigenschap()

fun createRESTZaakKenmerk() = RESTZaakKenmerk("Sample kenmerk", "Sample bron")

fun createRESTZaakToekennenGegevens(
    zaakUUID: UUID = UUID.randomUUID(),
    groepId: String = "dummyGroupId",
    behandelaarGebruikersnaam: String = "dummyBehandelaarGebruikersnaam",
    reden: String = "dummyReden"
) = RESTZaakToekennenGegevens().apply {
    this.zaakUUID = zaakUUID
    this.groepId = groepId
    this.behandelaarGebruikersnaam = behandelaarGebruikersnaam
    this.reden = reden
}

fun createRESTZaakRechten() = RESTZaakRechten()

fun createRESTZaakResultaat() = RESTZaakResultaat()

fun createRESTZaakStatus() = RESTZaakStatus().apply {
    naam = "Sample name"
    toelichting = "Sample toelichting"
}

fun createRESTZaaktype() = RESTZaaktype().apply {
    uuid = UUID.randomUUID()
    identificatie = "dummyIdentificatie"
    doel = "Sample Doel"
    omschrijving = ZAAK_TYPE_1_OMSCHRIJVING
    referentieproces = "Sample Referentieproces"
    servicenorm = true
    versiedatum = LocalDate.now()
    beginGeldigheid = LocalDate.of(2023, 1, 1)
    eindeGeldigheid = LocalDate.of(2023, 12, 31)
    vertrouwelijkheidaanduiding = Vertrouwelijkheidaanduiding.OPENBAAR
    nuGeldig = true
    opschortingMogelijk = false
    verlengingMogelijk = false
    verlengingstermijn = null
    zaaktypeRelaties = emptyList()
    informatieobjecttypes = emptyList()
    // use empty zaakafhandelparameters object here
    zaakafhandelparameters = RESTZaakafhandelParameters()
}

private fun createZaakData(): Map<String, Any> {
    val zaakdata = HashMap<String, Any>()
    zaakdata["key1"] = "value1"
    zaakdata["key2"] = 123
    zaakdata["key3"] = LocalDate.of(2023, 9, 14)
    return zaakdata
}
