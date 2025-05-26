/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import net.atos.zac.app.bag.model.RESTBAGObject
import net.atos.zac.app.bag.model.RESTOpenbareRuimte
import net.atos.zac.app.bag.model.RESTPand
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.admin.createRestZaakAfhandelParameters
import nl.info.zac.app.admin.model.RestZaakafhandelParameters
import nl.info.zac.app.identity.model.RestGroup
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.policy.model.RestZaakRechten
import nl.info.zac.search.model.ZaakIndicatie
import java.net.URI
import java.time.LocalDate
import java.util.EnumSet
import java.util.UUID

// note: the value of the zaak type 'omschrijving' field is used to determine whether users
// are allowed access to a zaak type
const val ZAAK_TYPE_1_OMSCHRIJVING = "zaaktype1"
const val ZAAK_TYPE_2_OMSCHRIJVING = "zaaktype2"

fun createRestDecision(
    url: URI = URI("http://localhost:8080/${UUID.randomUUID()}"),
    uuid: UUID = UUID.randomUUID()
) = RestDecision(
    url = url,
    uuid = uuid
)

@Suppress("LongParameterList")
fun createRestDecisionCreateData(
    zaakUuid: UUID = UUID.randomUUID(),
    resultaattypeUuid: UUID = UUID.randomUUID(),
    besluittypeUuid: UUID = UUID.randomUUID(),
    toelichting: String = "fakeToelichting",
    ingangsdatum: LocalDate = LocalDate.of(2023, 9, 14),
    vervaldatum: LocalDate = LocalDate.of(2023, 10, 5),
    publicationDate: LocalDate? = null,
    lastResponseDate: LocalDate? = null,
    informatieobjecten: List<UUID> = listOf(UUID.randomUUID())
) =
    RestDecisionCreateData(
        besluittypeUuid = besluittypeUuid,
        informatieobjecten = informatieobjecten,
        ingangsdatum = ingangsdatum,
        resultaattypeUuid = resultaattypeUuid,
        toelichting = toelichting,
        vervaldatum = vervaldatum,
        zaakUuid = zaakUuid,
        publicationDate = publicationDate,
        lastResponseDate = lastResponseDate
    )

@Suppress("LongParameterList")
fun createRestDecisionChangeData(
    besluitUUID: UUID = UUID.randomUUID(),
    resultTypeUUID: UUID = UUID.randomUUID(),
    description: String = "besluitFakeDescription",
    effectiveDate: LocalDate = LocalDate.of(2023, 9, 14),
    expirationDate: LocalDate = LocalDate.of(2023, 11, 14),
    publicationDate: LocalDate = LocalDate.of(2023, 10, 14),
    lastResponseDate: LocalDate = LocalDate.of(2023, 11, 1),
    informationObjects: List<UUID> = listOf(UUID.randomUUID()),
    reason: String = "fakeReason"
) = RestDecisionChangeData(
    besluitUuid = besluitUUID,
    resultaattypeUuid = resultTypeUUID,
    toelichting = description,
    ingangsdatum = effectiveDate,
    vervaldatum = expirationDate,
    publicationDate = publicationDate,
    lastResponseDate = lastResponseDate,
    informatieobjecten = informationObjects,
    reden = reason
)

fun createRESTGerelateerdeZaak() = RestGerelateerdeZaak()

fun createRESTGeometry(
    type: String = "Point",
    point: RestCoordinates = createRestCoordinates()
) = RestGeometry(
    type = type,
    point = point
)

fun createRestGroup(
    id: String = "fakeId",
    name: String = "fakeName",
) = RestGroup(
    id = id,
    naam = name
)

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

fun createRestUser(
    id: String = "fakeId",
    name: String = "fakeName",
) = RestUser(
    id = id,
    naam = name
)

@Suppress("LongParameterList")
fun createRestZaak(
    behandelaar: RestUser = createRestUser(),
    restGroup: RestGroup = createRestGroup(),
    indicaties: EnumSet<ZaakIndicatie> = EnumSet.noneOf(ZaakIndicatie::class.java),
    restZaakType: RestZaaktype = createRestZaaktype(),
    uiterlijkeEinddatumAfdoening: LocalDate = LocalDate.of(2023, 10, 10),
    uuid: UUID = UUID.randomUUID(),
    rechten: RestZaakRechten = createRestZaakRechten(),
    einddatumGepland: LocalDate? = LocalDate.of(2023, 10, 10),
    communicatiekanaal: String? = "fakeCommunicatiekanaal",
    isOpen: Boolean = true,
    startDatum: LocalDate = LocalDate.of(2023, 9, 15),
    initiatorIdentificatieType: IdentificatieType = IdentificatieType.BSN,
) = RestZaak(
    uuid = uuid,
    identificatie = "ZA2023001",
    omschrijving = "Sample Zaak",
    toelichting = "This is a test zaak",
    zaaktype = restZaakType,
    status = createRestZaakStatus(),
    resultaat = createRestZaakResultaat(),
    besluiten = listOf(createRestDecision()),
    bronorganisatie = "Sample Bronorganisatie",
    verantwoordelijkeOrganisatie = "Sample Verantwoordelijke Organisatie",
    registratiedatum = LocalDate.of(2023, 9, 14),
    startdatum = startDatum,
    einddatumGepland = einddatumGepland,
    einddatum = LocalDate.of(2023, 10, 5),
    uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening,
    publicatiedatum = LocalDate.of(2023, 9, 16),
    archiefActiedatum = LocalDate.of(2023, 10, 15),
    archiefNominatie = "Sample Archief Nominatie",
    communicatiekanaal = communicatiekanaal,
    vertrouwelijkheidaanduiding = "Sample Vertrouwelijkheidaanduiding",
    zaakgeometrie = createRESTGeometry(),
    isOpgeschort = true,
    isEerderOpgeschort = false,
    redenOpschorting = "Sample Reden Opschorting",
    isVerlengd = true,
    redenVerlenging = "Sample Reden Verlenging",
    duurVerlenging = "Sample Duur Verlenging",
    groep = restGroup,
    behandelaar = behandelaar,
    gerelateerdeZaken = listOf(createRESTGerelateerdeZaak()),
    kenmerken = listOf(createRESTZaakKenmerk()),
    zaakdata = createZaakData(),
    indicaties = indicaties,
    initiatorIdentificatieType = initiatorIdentificatieType,
    initiatorIdentificatie = "Sample Initiator Identificatie",
    isOpen = isOpen,
    isHeropend = false,
    isHoofdzaak = true,
    isDeelzaak = false,
    isBesluittypeAanwezig = false,
    isInIntakeFase = true,
    isProcesGestuurd = false,
    rechten = rechten
)

fun createRESTZaakAanmaakGegevens(
    zaakTypeUUID: UUID = UUID.randomUUID(),
    zaak: RestZaak = createRestZaak(
        restZaakType = RestZaaktype(
            // we only need a UUID for the zaaktype when creating a zaak
            uuid = zaakTypeUUID
        )
    ),
    inboxProductaanvraag: RESTInboxProductaanvraag = createRESTInboxProductaanvraag(),
    bagObjecten: List<RESTBAGObject> = listOf(createRESTPand(), createRESTOpenbareRuimte())
) = RESTZaakAanmaakGegevens(
    zaak = zaak,
    inboxProductaanvraag = inboxProductaanvraag,
    bagObjecten = bagObjecten
)

fun createRESTZaakBetrokkeneGegevens(
    zaakUUID: UUID = UUID.randomUUID(),
    roltypeUUID: UUID = UUID.randomUUID(),
    roltoelichting: String = "fakeRoltoelichting",
    betrokkeneIdentificatieType: IdentificatieType = IdentificatieType.BSN,
    betrokkeneIdentificatie: String = "fakeBetrokkeneIdentificatie"
) = RESTZaakBetrokkeneGegevens(
    zaakUUID = zaakUUID,
    roltypeUUID = roltypeUUID,
    roltoelichting = roltoelichting,
    betrokkeneIdentificatieType = betrokkeneIdentificatieType,
    betrokkeneIdentificatie = betrokkeneIdentificatie
)

fun createRESTZaakKenmerk() = RESTZaakKenmerk("Sample kenmerk", "Sample bron")

fun createRESTZaakAssignmentData(
    zaakUUID: UUID = UUID.randomUUID(),
    groepId: String = "fakeGroupId",
    behandelaarGebruikersnaam: String = "fakeBehandelaarGebruikersnaam",
    reden: String = "fakeReden"
) = RestZaakAssignmentData(
    zaakUUID = zaakUUID,
    groupId = groepId,
    assigneeUserName = behandelaarGebruikersnaam,
    reason = reden
)

fun createRESTZakenVerdeelGegevens(
    uuids: List<UUID> = emptyList(),
    groepId: String = "fakeGroupId",
    behandelaarGebruikersnaam: String? = null,
    reden: String? = null,
    screenEventResourceId: String? = null
) = RESTZakenVerdeelGegevens(
    uuids = uuids,
    groepId = groepId,
    behandelaarGebruikersnaam = behandelaarGebruikersnaam,
    reden = reden,
    screenEventResourceId = screenEventResourceId
)

fun createRestZaakLinkData(
    zaakUuid: UUID = UUID.randomUUID(),
    teKoppelenZaakUuid: UUID = UUID.randomUUID(),
    relatieType: RelatieType,
    reverseRelatieType: RelatieType? = null
) = RestZaakLinkData(
    zaakUuid = zaakUuid,
    teKoppelenZaakUuid = teKoppelenZaakUuid,
    relatieType = relatieType,
    reverseRelatieType = reverseRelatieType
)

fun createRestZaakUnlinkData(
    zaakUuid: UUID = UUID.randomUUID(),
    gekoppeldeZaakIdentificatie: String = "fakeIdentificatie",
    relationType: RelatieType,
    reason: String = "fakeReason"
) = RestZaakUnlinkData(
    zaakUuid = zaakUuid,
    gekoppeldeZaakIdentificatie = gekoppeldeZaakIdentificatie,
    relatieType = relationType,
    reden = reason
)

@Suppress("LongParameterList")
fun createRestZaakRechten(
    lezen: Boolean = true,
    wijzigen: Boolean = true,
    toekennen: Boolean = true,
    behandelen: Boolean = true,
    afbreken: Boolean = true,
    heropenen: Boolean = true,
    bekijkenZaakdata: Boolean = true,
    wijzigenDoorlooptijd: Boolean = true,
    toevoegenBagObject: Boolean = true,
    toevoegenBetrokkeneBedrijf: Boolean = true,
    toevoegenBetrokkenePersoon: Boolean = true,
    toevoegenInitiatorBedrijf: Boolean = true,
    toevoegenInitiatorPersoon: Boolean = true,
    versturenOntvangstbevestiging: Boolean = true,
    verwijderenBetrokkene: Boolean = true,
    verwijderenInitiator: Boolean = true,
    creeerenDocument: Boolean = true,
    versturenEmail: Boolean = true,
    wijzigenLocatie: Boolean = true
) = RestZaakRechten(
    lezen = lezen,
    wijzigen = wijzigen,
    toekennen = toekennen,
    behandelen = behandelen,
    afbreken = afbreken,
    heropenen = heropenen,
    bekijkenZaakdata = bekijkenZaakdata,
    wijzigenDoorlooptijd = wijzigenDoorlooptijd,
    toevoegenBagObject = toevoegenBagObject,
    toevoegenBetrokkeneBedrijf = toevoegenBetrokkeneBedrijf,
    toevoegenBetrokkenePersoon = toevoegenBetrokkenePersoon,
    toevoegenInitiatorBedrijf = toevoegenInitiatorBedrijf,
    toevoegenInitiatorPersoon = toevoegenInitiatorPersoon,
    versturenOntvangstbevestiging = versturenOntvangstbevestiging,
    verwijderenBetrokkene = verwijderenBetrokkene,
    verwijderenInitiator = verwijderenInitiator,
    creeerenDocument = creeerenDocument,
    versturenEmail = versturenEmail,
    wijzigenLocatie = wijzigenLocatie
)

fun createRestZaakResultaat() = RestZaakResultaat()

fun createRestZaakStatus(
    naam: String = "fakeName",
    toelichting: String = "fakeToelichting"
) = RestZaakStatus(
    naam = naam,
    toelichting = toelichting
)

fun createRestZaaktype(
    zaakafhandelParameters: RestZaakafhandelParameters = createRestZaakAfhandelParameters()
) = RestZaaktype(
    uuid = UUID.randomUUID(),
    identificatie = "fakeIdentificatie",
    doel = "Sample Doel",
    omschrijving = ZAAK_TYPE_1_OMSCHRIJVING,
    referentieproces = "Sample Referentieproces",
    servicenorm = true,
    versiedatum = LocalDate.now(),
    beginGeldigheid = LocalDate.of(2023, 1, 1),
    eindeGeldigheid = LocalDate.of(2023, 12, 31),
    vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    nuGeldig = true,
    opschortingMogelijk = false,
    verlengingMogelijk = false,
    verlengingstermijn = null,
    zaaktypeRelaties = emptyList(),
    informatieobjecttypes = emptyList(),
    zaakafhandelparameters = zaakafhandelParameters
)

private fun createZaakData() = mapOf(
    "key1" to "value1",
    "key2" to 123,
    "key3" to LocalDate.of(2023, 9, 14)
)

fun createRESTZaakOverzicht(
    uuid: UUID = UUID.randomUUID()
) = RestZaakOverzicht().apply {
    this.uuid = uuid
}

fun createRestZaakLocatieGegevens(
    restGeometry: RestGeometry? = createRESTGeometry(),
    reason: String = "fakeReden"
) = RestZaakLocatieGegevens(
    geometrie = restGeometry,
    reden = reason
)

fun createRestCoordinates(
    latitude: Double = 52.378,
    longitude: Double = 4.900
) = RestCoordinates(
    latitude = latitude,
    longitude = longitude
)

fun createRestDocumentOntkoppelGegevens(
    zaakUUID: UUID = UUID.randomUUID(),
    documentUUID: UUID = UUID.randomUUID(),
    reden: String = "fakeReason“"
) = RESTDocumentOntkoppelGegevens(
    zaakUUID = zaakUUID,
    documentUUID = documentUUID,
    reden = reden
)
