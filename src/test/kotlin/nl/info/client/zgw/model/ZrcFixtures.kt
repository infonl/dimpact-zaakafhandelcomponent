/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.model

import net.atos.client.zgw.zrc.model.AardRelatieWeergave
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.Opschorting
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Resultaat
import nl.info.client.zgw.zrc.model.generated.Verlenging
import nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.generated.RolType
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
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
    indicatie: Boolean = false
) = Opschorting().apply {
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
    betrokkeneIdentificatie: MedewerkerIdentificatie? = createMedewerkerIdentificatie()
) = RolMedewerker(
    zaakURI,
    rolType,
    roltoelichting,
    betrokkeneIdentificatie
)

fun createRolMedewerkerForReads(
    uuid: UUID = UUID.randomUUID(),
    rolType: RolType = createRolType(),
) = RolMedewerker(
    uuid,
    rolType
)

fun createRolNatuurlijkPersoon(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(zaakTypeUri = zaakURI),
    toelichting: String = "fakeToelichting",
    natuurlijkPersoon: NatuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie()
) = RolNatuurlijkPersoon(
    zaakURI,
    rolType,
    toelichting,
    natuurlijkPersoon
)

fun createRolOrganisatorischeEenheid(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(),
    toelichting: String = "fakeToelichting",
    organisatorischeEenheid: OrganisatorischeEenheidIdentificatie = createOrganisatorischeEenheid()
) = RolOrganisatorischeEenheid(
    zaakURI,
    rolType,
    toelichting,
    organisatorischeEenheid
)

fun createRolOrganisatorischeEenheidForReads(
    uuid: UUID = UUID.randomUUID(),
    rolType: RolType = createRolType(),
) = RolOrganisatorischeEenheid(
    uuid,
    rolType
)

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
    aardRelatieWeergave: AardRelatieWeergave = AardRelatieWeergave.HOORT_BIJ,
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
    zaak: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    statustype: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    datumStatusGezet: ZonedDateTime = ZonedDateTime.now()
) = Status(uri, uuid, zaak, statustype, datumStatusGezet)

fun createVerlenging(
    reden: String = "fakeReden",
    duur: String = "0"
) = Verlenging().apply {
    this.reden = reden
    this.duur = duur
}
