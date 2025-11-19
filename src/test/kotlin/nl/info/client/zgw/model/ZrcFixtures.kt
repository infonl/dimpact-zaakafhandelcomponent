/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.model

import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.AardRelatieWeergaveEnum
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.Opschorting
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Resultaat
import nl.info.client.zgw.zrc.model.generated.Status
import nl.info.client.zgw.zrc.model.generated.Verlenging
import nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.VestigingIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.generated.RolType
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

fun createMedewerkerIdentificatie(
    identificatie: String = "fakeIdentificatie",
    achternaam: String = "fakeAchternaam",
    voorletters: String = "fakeVoorletters",
    voorvoegselAchternaam: String? = null
) = MedewerkerIdentificatie().apply {
    this.identificatie = identificatie
    this.achternaam = achternaam
    this.voorletters = voorletters
    this.voorvoegselAchternaam = voorvoegselAchternaam
}

fun createNatuurlijkPersoonIdentificatie(bsn: String = "fakeBsn") = NatuurlijkPersoonIdentificatie().apply {
    this.inpBsn = bsn
}

fun createNietNatuurlijkPersoonIdentificatie(
    innNnpId: String? = null,
    vestigingsnummer: String? = "123456789123",
    kvkNummer: String? = "12345678",
    annIdentificatie: String? = null
) = NietNatuurlijkPersoonIdentificatie().apply {
    this.innNnpId = innNnpId
    this.vestigingsNummer = vestigingsnummer
    this.kvkNummer = kvkNummer
    this.annIdentificatie = annIdentificatie
}

fun createOrganisatorischeEenheidIdentificatie(
    identificatie: String = "fakeIdentificatie",
    naam: String = "fakeNaam"
) = OrganisatorischeEenheidIdentificatie().apply {
    this.identificatie = identificatie
    this.naam = naam
}

fun createObjectOpenbareRuimte(
    identificatie: String = "fakeIdentificatie",
    openbareRuimteNaam: String = "fakeopenbareRuimteNaam",
    plaatsNaam: String = "fakePlaatsNaam"
) = ObjectOpenbareRuimte(
    identificatie,
    openbareRuimteNaam,
    plaatsNaam
)

fun createObjectPand(identificatie: String = "fakeIdentificatie") = ObjectPand(identificatie)

fun createOpschorting(
    reden: String? = null,
    indicatie: Boolean = false,
    eerdereOpschorting: Boolean = false
) = Opschorting(eerdereOpschorting).apply {
    this.reden = reden
    this.indicatie = indicatie
}

fun createOrganisatorischeEenheid(
    identificatie: String = "fakeIdentificatie",
    naam: String = "fakeNaam"
) = OrganisatorischeEenheidIdentificatie().apply {
    this.identificatie = identificatie
    this.naam = naam
}

fun createGeoJSONGeometryWithDeletionSupport(
    longitude: BigDecimal = BigDecimal("4.56"),
    latitude: BigDecimal = BigDecimal("1.23")
) = DeleteGeoJSONGeometry().apply {
    this.type = GeometryTypeEnum.POINT
    this.coordinates = listOf(
        longitude,
        latitude
    )
}

fun createResultaat(
    url: URI = URI("http://example.com/resultaat/${UUID.randomUUID()}"),
    uuid: UUID = UUID.randomUUID(),
    resultaatTypeURI: URI = URI("http://example.com/resultaattype/${UUID.randomUUID()}")
) = Resultaat(
    url,
    uuid
).apply {
    resultaattype = resultaatTypeURI
}

fun createRolMedewerker(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(),
    roltoelichting: String = "fakeToelichting",
    medewerkerIdentificatie: MedewerkerIdentificatie? = createMedewerkerIdentificatie()
) = RolMedewerker(
    zaakURI,
    rolType,
    roltoelichting,
    medewerkerIdentificatie
)

fun createRolMedewerkerForReads(
    uuid: UUID = UUID.randomUUID(),
    rolType: RolType = createRolType(),
    roltoelichting: String = "fakeToelichting",
    medewerkerIdentificatie: MedewerkerIdentificatie = createMedewerkerIdentificatie()
) = RolMedewerker(
    uuid,
    rolType,
    roltoelichting,
    medewerkerIdentificatie
)

fun createRolNatuurlijkPersoon(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(zaakTypeUri = zaakURI),
    toelichting: String = "fakeToelichting",
    natuurlijkPersoonIdentificatie: NatuurlijkPersoonIdentificatie? = createNatuurlijkPersoonIdentificatie()
) = RolNatuurlijkPersoon(
    zaakURI,
    rolType,
    toelichting,
    natuurlijkPersoonIdentificatie
)

fun createRolNatuurlijkPersoonForReads(
    uuid: UUID = UUID.randomUUID(),
    rolType: RolType = createRolType(),
    toelichting: String = "fakeToelichting",
    natuurlijkPersoonIdentificatie: NatuurlijkPersoonIdentificatie? = createNatuurlijkPersoonIdentificatie()
) = RolNatuurlijkPersoon(
    uuid,
    rolType,
    toelichting,
    natuurlijkPersoonIdentificatie
)

fun createRolNietNatuurlijkPersoon(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(zaakTypeUri = zaakURI),
    toelichting: String = "fakeToelichting",
    nietNatuurlijkPersoonIdentificatie: NietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie()
) = RolNietNatuurlijkPersoon(
    zaakURI,
    rolType,
    toelichting,
    nietNatuurlijkPersoonIdentificatie
)

fun createRolNietNatuurlijkPersoonForReads(
    uuid: UUID = UUID.randomUUID(),
    rolType: RolType = createRolType(),
    toelichting: String = "fakeToelichting",
    nietNatuurlijkPersoonIdentificatie: NietNatuurlijkPersoonIdentificatie? = createNietNatuurlijkPersoonIdentificatie()
) = RolNietNatuurlijkPersoon(
    uuid,
    rolType,
    toelichting,
    nietNatuurlijkPersoonIdentificatie
)

fun createRolOrganisatorischeEenheid(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(),
    toelichting: String = "fakeToelichting",
    organisatorischeEenheidIdentificatie: OrganisatorischeEenheidIdentificatie = createOrganisatorischeEenheid()
) = RolOrganisatorischeEenheid(
    zaakURI,
    rolType,
    toelichting,
    organisatorischeEenheidIdentificatie
)

fun createRolOrganisatorischeEenheidForReads(
    uuid: UUID = UUID.randomUUID(),
    rolType: RolType = createRolType(),
    roltoelichting: String = "fakeToelichting",
    organisatorischeEenheidIdentificatie: OrganisatorischeEenheidIdentificatie? =
        createOrganisatorischeEenheidIdentificatie()
) = RolOrganisatorischeEenheid(
    uuid,
    rolType,
    roltoelichting,
    organisatorischeEenheidIdentificatie
)

fun createRolVestiging(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(),
    toelichting: String = "fakeToelichting",
    vestigingIdentificatie: VestigingIdentificatie = createVestigingIdentificatie()
) = RolVestiging(
    zaakURI,
    rolType,
    toelichting,
    vestigingIdentificatie
)

fun createVestigingIdentificatie(
    vestigingsNummer: String = "fakeVestigingsNummer",
    handelsnaam: List<String>? = listOf("fakeHandelsnaam1", "fakeHandelsnaam2"),
    kvkNummer: String = "fakeKvkNummer"
) = VestigingIdentificatie().apply {
    this.vestigingsNummer = vestigingsNummer
    this.handelsnaam = handelsnaam
    this.kvkNummer = kvkNummer
}

@Suppress("LongParameterList")
fun createZaak(
    uuid: UUID = UUID.randomUUID(),
    zaakTypeURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    startDate: LocalDate = LocalDate.now(),
    endDate: LocalDate? = null,
    bronOrganisatie: String = "fakeBronOrganisatie",
    verantwoordelijkeOrganisatie: String = "fakeVerantwoordelijkeOrganisatie",
    // an archiefnominatie which is not null means that the zaak is closed
    archiefnominatie: ArchiefnominatieEnum? = null,
    opschorting: Opschorting? = null,
    einddatumGepland: LocalDate? = null,
    identificatie: String = "fakeIdentificatie",
    registratiedatum: LocalDate = LocalDate.now(),
    resultaat: URI? = null,
    uiterlijkeEinddatumAfdoening: LocalDate = LocalDate.now().plusDays(1),
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    status: URI? = null,
    verlenging: Verlenging? = null,
    hoofdzaakUri: URI? = null,
    deelzaken: List<URI>? = null,
    omschrijving: String = "fakeOmschrijving"
) = Zaak(
    URI("https://example.com/zaak/$uuid"),
    uuid,
    endDate,
    null,
    deelzaken,
    null,
    null,
    status,
    null,
    null,
    resultaat
).apply {
    this.zaaktype = zaakTypeURI
    this.startdatum = startDate
    this.archiefnominatie = archiefnominatie
    this.opschorting = opschorting
    this.einddatumGepland = einddatumGepland
    this.identificatie = identificatie
    this.registratiedatum = registratiedatum
    this.uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.verlenging = verlenging
    this.hoofdzaak = hoofdzaakUri
    this.omschrijving = omschrijving
    this.bronorganisatie = bronOrganisatie
    this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie
}

fun createZaakobjectOpenbareRuimte(
    zaakURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    bagobjectURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    objectOpenbareRuimte: ObjectOpenbareRuimte = createObjectOpenbareRuimte()
) = ZaakobjectOpenbareRuimte(
    zaakURI,
    bagobjectURI,
    objectOpenbareRuimte
)

fun createZaakInformatieobjectForCreatesAndUpdates(
    informatieobjectUUID: UUID = UUID.randomUUID(),
    zaakUUID: UUID = UUID.randomUUID(),
    informatieObjectURL: URI = URI("https://example.com/$informatieobjectUUID"),
    zaakURL: URI = URI("https://example.com/$zaakUUID")
) = ZaakInformatieobject(
    informatieObjectURL,
    zaakURL
)

fun createZaakInformatieobjectForReads(
    url: URI = URI("https://example.com/${UUID.randomUUID()}"),
    uuid: UUID = UUID.randomUUID(),
    aardRelatieWeergave: AardRelatieWeergaveEnum = AardRelatieWeergaveEnum.HOORT_BIJ_OMGEKEERD_KENT,
    registratiedatum: ZonedDateTime = ZonedDateTime.now()
) = ZaakInformatieobject(
    url,
    uuid,
    aardRelatieWeergave,
    registratiedatum
)

fun createZaakobjectProductaanvraag(
    zaakURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    productaanvraagURI: URI = URI("http://example.com/${UUID.randomUUID()}")
) =
    ZaakobjectProductaanvraag(
        zaakURI,
        productaanvraagURI
    )

fun createZaakobjectPand(
    zaakURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    bagobjectURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    objectPand: ObjectPand = createObjectPand()
) =
    ZaakobjectPand(
        zaakURI,
        bagobjectURI,
        objectPand
    )

fun createZaakStatus(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    zaakURI: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    statustypeURI: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    datumStatusGezet: OffsetDateTime = ZonedDateTime.now().toOffsetDateTime()
) = Status(
    uri,
    uuid,
    false,
    emptyList()
).apply {
    this.zaak = zaakURI
    this.statustype = statustypeURI
    this.datumStatusGezet = datumStatusGezet
}

fun createVerlenging(
    reden: String = "fakeReden",
    duur: String = "0"
) = Verlenging().apply {
    this.reden = reden
    this.duur = duur
}
