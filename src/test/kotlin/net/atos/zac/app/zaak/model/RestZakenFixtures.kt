/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak.model

import net.atos.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.zac.app.admin.createRestZaakAfhandelParameters
import net.atos.zac.app.bag.model.RESTBAGObject
import net.atos.zac.app.bag.model.RESTOpenbareRuimte
import net.atos.zac.app.bag.model.RESTPand
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.policy.model.RESTZaakRechten
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import net.atos.zac.zoeken.model.ZaakIndicatie
import java.net.URI
import java.time.LocalDate
import java.util.EnumSet
import java.util.UUID

// note: the value of the zaak type 'omschrijving' field is used to determine whether users
// are allowed access to a zaak type
const val ZAAK_TYPE_1_OMSCHRIJVING = "zaaktype1"
const val ZAAK_TYPE_2_OMSCHRIJVING = "zaaktype2"

fun createRESTBesluit(
    url: URI = URI("http://localhost:8080/${UUID.randomUUID()}"),
    uuid: UUID = UUID.randomUUID()
) = RestBesluit(
    url = url,
    uuid = uuid
)

@Suppress("LongParameterList")
fun createRESTBesluitVastleggenGegevens(
    zaakUuid: UUID = UUID.randomUUID(),
    resultaattypeUuid: UUID = UUID.randomUUID(),
    besluittypeUuid: UUID = UUID.randomUUID(),
    toelichting: String = "dummyToelichting",
    ingangsdatum: LocalDate = LocalDate.of(2023, 9, 14),
    vervaldatum: LocalDate = LocalDate.of(2023, 10, 5),
    informatieobjecten: List<UUID> = listOf(UUID.randomUUID())
) =
    RestBesluitVastleggenGegevens(
        besluittypeUuid = besluittypeUuid,
        informatieobjecten = informatieobjecten,
        ingangsdatum = ingangsdatum,
        resultaattypeUuid = resultaattypeUuid,
        toelichting = toelichting,
        vervaldatum = vervaldatum,
        zaakUuid = zaakUuid
    )

fun createRESTGerelateerdeZaak() = RestGerelateerdeZaak()

fun createRESTGeometry(
    type: String = "dummyType",
) = RestGeometry(
    type = type
)

fun createRESTGroup(
    id: String = "dummyId",
    name: String = "dummyName",
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

fun createRESTUser(
    id: String = "dummyId",
    name: String = "dummyName",
) = RestUser(
    id = id,
    naam = name
)

fun createRESTZaak(
    behandelaar: RestUser = createRESTUser(),
    restGroup: RestGroup = createRESTGroup(),
    indicaties: EnumSet<ZaakIndicatie> = EnumSet.noneOf(ZaakIndicatie::class.java),
    restZaakType: RestZaaktype = createRESTZaaktype(),
    uiterlijkeEinddatumAfdoening: LocalDate = LocalDate.of(2023, 10, 10)
) = RestZaak(
    uuid = UUID.randomUUID(),
    identificatie = "ZA2023001",
    omschrijving = "Sample Zaak",
    toelichting = "This is a test zaak",
    zaaktype = restZaakType,
    status = createRESTZaakStatus(),
    resultaat = createRESTZaakResultaat(),
    besluiten = listOf(createRESTBesluit()),
    bronorganisatie = "Sample Bronorganisatie",
    verantwoordelijkeOrganisatie = "Sample Verantwoordelijke Organisatie",
    registratiedatum = LocalDate.of(2023, 9, 14),
    startdatum = LocalDate.of(2023, 9, 15),
    einddatumGepland = LocalDate.of(2023, 10, 1),
    einddatum = LocalDate.of(2023, 10, 5),
    uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening,
    publicatiedatum = LocalDate.of(2023, 9, 16),
    archiefActiedatum = LocalDate.of(2023, 10, 15),
    archiefNominatie = "Sample Archief Nominatie",
    communicatiekanaal = "dummyCommunicatiekanaal",
    vertrouwelijkheidaanduiding = "Sample Vertrouwelijkheidaanduiding",
    zaakgeometrie = createRESTGeometry(),
    isOpgeschort = true,
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
    initiatorIdentificatieType = IdentificatieType.BSN,
    initiatorIdentificatie = "Sample Initiator Identificatie",
    isOpen = true,
    isHeropend = false,
    isHoofdzaak = true,
    isDeelzaak = false,
    isOntvangstbevestigingVerstuurd = true,
    isBesluittypeAanwezig = false,
    isInIntakeFase = true,
    isProcesGestuurd = false,
    rechten = createRESTZaakRechten()
)

fun createRESTZaakAanmaakGegevens(
    zaakTypeUUID: UUID = UUID.randomUUID(),
    zaak: RestZaak = createRESTZaak(
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

fun createRESTZaakKenmerk() = RESTZaakKenmerk("Sample kenmerk", "Sample bron")

fun createRESTZaakToekennenGegevens(
    zaakUUID: UUID = UUID.randomUUID(),
    groepId: String = "dummyGroupId",
    behandelaarGebruikersnaam: String = "dummyBehandelaarGebruikersnaam",
    reden: String = "dummyReden"
) = RestZaakAssignmentData(
    zaakUUID = zaakUUID,
    groupId = groepId,
    assigneeUserName = behandelaarGebruikersnaam,
    reason = reden
)

fun createRESTZakenVerdeelGegevens(
    uuids: List<UUID> = emptyList(),
    groepId: String = "dummyGroupId",
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

fun createRESTZaakKoppelGegevens(
    zaakUuid: UUID = UUID.randomUUID(),
    teKoppelenZaakUuid: UUID = UUID.randomUUID(),
    relatieType: RelatieType,
    reverseRelatieType: RelatieType
) = RESTZaakKoppelGegevens(
    zaakUuid = zaakUuid,
    teKoppelenZaakUuid = teKoppelenZaakUuid,
    relatieType = relatieType,
    reverseRelatieType = reverseRelatieType
)

fun createRESTZaakRechten() = RESTZaakRechten()

fun createRESTZaakResultaat() = RESTZaakResultaat()

fun createRESTZaakStatus(
    naam: String = "dummyName",
    toelichting: String = "dummyToelichting"
) = RestZaakStatus(
    naam = naam,
    toelichting = toelichting
)

fun createRESTZaaktype() = RestZaaktype(
    uuid = UUID.randomUUID(),
    identificatie = "dummyIdentificatie",
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
    zaakafhandelparameters = createRestZaakAfhandelParameters()
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
