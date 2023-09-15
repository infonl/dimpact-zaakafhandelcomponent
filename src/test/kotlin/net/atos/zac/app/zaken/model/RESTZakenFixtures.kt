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


// the value of the zaak type 'omschrijving' field is used to determine whether users are allowed access
// to a zaak type or not
const val ZAAK_TYPE_1_OMSCHRIJVING = "zaaktype1"
const val ZAAK_TYPE_2_OMSCHRIJVING = "zaaktype2"

fun createRESTZaak() = RESTZaak().apply {
    uuid = UUID.randomUUID()
    identificatie = "ZA2023001"
    omschrijving = "Sample Zaak"
    toelichting = "This is a test zaak"
    zaaktype = createRESTZaaktype()
    status = createSampleRESTZaakStatus()
    resultaat = createSampleRESTZaakResultaat()
    besluiten = listOf(createSampleBesluit())
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
    communicatiekanaal = createSampleRESTCommunicatiekanaal()
    vertrouwelijkheidaanduiding = "Sample Vertrouwelijkheidaanduiding"
    zaakgeometrie = createSampleRESTGeometry()
    isOpgeschort = true
    redenOpschorting = "Sample Reden Opschorting"
    isVerlengd = true
    redenVerlenging = "Sample Reden Verlenging"
    duurVerlenging = "Sample Duur Verlenging"
    groep = createSampleRESTGroup()
    behandelaar = createSampleRESTUser()
    gerelateerdeZaken = listOf(createSampleRESTGerelateerdeZaak())
    kenmerken = listOf(createSampleRESTZaakKenmerk())
    eigenschappen = listOf(createSampleRESTZaakEigenschap())
    zaakdata = createSampleZaakdata()
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
    rechten = createSampleRESTZaakRechten()
}

fun createRESTZaakAanmaakGegevens() = RESTZaakAanmaakGegevens().apply {
    zaak = createRESTZaak()
    inboxProductaanvraag = createRESTInboxProductaanvraag()
    bagObjecten = listOf(createRESTPand(), createRESTOpenbareRuimte())
}

fun createRESTZaakafhandelParameters() = RESTZaakafhandelParameters()

fun createRESTInboxProductaanvraag() = RESTInboxProductaanvraag()

fun createRESTPand() = RESTPand()

fun createRESTOpenbareRuimte() = RESTOpenbareRuimte()

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

private fun createSampleRESTZaakStatus() = RESTZaakStatus().apply {
    naam = "Sample name"
    toelichting = "Sample toelichting"
}

private fun createSampleRESTZaakResultaat() = RESTZaakResultaat()

private fun createSampleRESTCommunicatiekanaal() = RESTCommunicatiekanaal()

private fun createSampleRESTGeometry() = RESTGeometry()

private fun createSampleRESTGroup() = RESTGroup()

private fun createSampleRESTUser() = RESTUser()

private fun createSampleBesluit() = RESTBesluit()

private fun createSampleRESTGerelateerdeZaak() = RESTGerelateerdeZaak()

private fun createSampleRESTZaakKenmerk() = RESTZaakKenmerk("Sample kenmerk", "Sample bron")

private fun createSampleRESTZaakEigenschap() = RESTZaakEigenschap()

private fun createSampleRESTZaakRechten() = RESTZaakRechten()

private fun createSampleZaakdata(): Map<String, Any> {
    val zaakdata = HashMap<String, Any>()
    zaakdata["key1"] = "value1"
    zaakdata["key2"] = 123
    zaakdata["key3"] = LocalDate.of(2023, 9, 14)
    return zaakdata
}
