/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.model

import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Point2D
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.Verlenging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.generated.Opschorting
import net.atos.client.zgw.zrc.model.generated.Resultaat
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.model.generated.RolType
import nl.info.client.zgw.ztc.model.createRolType
import java.net.URI
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.util.UUID

fun createMedewerker(
    identificatie: String = "dummyIdentificatie",
    achternaam: String = "dummyAchternaam",
    voorletters: String = "dummyVoorletters",
    voorvoegselAchternaam: String? = null
) = Medewerker().apply {
    this.identificatie = identificatie
    this.achternaam = achternaam
    this.voorletters = voorletters
    this.voorvoegselAchternaam = voorvoegselAchternaam
}

fun createNatuurlijkPersoon(bsn: String = "dummyBsn") = NatuurlijkPersoon(bsn)

fun createObjectOpenbareRuimte(
    identificatie: String = "dummyIdentificatie",
    openbareRuimteNaam: String = "dummyopenbareRuimteNaam",
    plaatsNaam: String = "dummyPlaatsNaam"
) = ObjectOpenbareRuimte(
    identificatie,
    openbareRuimteNaam,
    plaatsNaam
)

fun createObjectPand(identificatie: String = "dummyIdentificatie") = ObjectPand(identificatie)

fun createOpschorting(
    reden: String? = null,
    indicatie: Boolean = false
) = Opschorting().apply {
    this.reden = reden
    this.indicatie = indicatie
}

fun createOrganisatorischeEenheid(
    identificatie: String = "dummyIdentificatie",
    naam: String = "dummyNaam"
) = OrganisatorischeEenheid().apply {
    this.identificatie = identificatie
    this.naam = naam
}

fun createPoint(
    coordinates: Point2D = createPoint2D()
) = Point(coordinates)

fun createPoint2D(
    latitude: Double = 1.23,
    longitude: Double = 4.56
) = Point2D(
    latitude,
    longitude
)

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
    roltoelichting: String = "dummyToelichting",
    betrokkeneIdentificatie: Medewerker? = createMedewerker()
) = RolMedewerker(
    zaakURI,
    rolType,
    roltoelichting,
    betrokkeneIdentificatie
)

fun createRolNatuurlijkPersoon(
    zaaktypeURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(zaakTypeUri = zaaktypeURI),
    toelichting: String = "dummyToelichting",
    natuurlijkPersoon: NatuurlijkPersoon = createNatuurlijkPersoon()
) = RolNatuurlijkPersoon(
    zaaktypeURI,
    rolType,
    toelichting,
    natuurlijkPersoon
)

fun createRolOrganisatorischeEenheid(
    zaakURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(),
    toelichting: String = "dummyToelichting",
    organisatorischeEenheid: OrganisatorischeEenheid = createOrganisatorischeEenheid()
) = RolOrganisatorischeEenheid(
    zaakURI,
    rolType,
    toelichting,
    organisatorischeEenheid
)

@Suppress("LongParameterList")
fun createZaak(
    zaakTypeURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    startDate: LocalDate = LocalDate.now(),
    bronOrganisatie: String = "dummyBronOrganisatie",
    verantwoordelijkeOrganisatie: String = "dummyVerantwoordelijkeOrganisatie",
    // an archiefnominatie which is not null means that the zaak is closed
    archiefnominatie: Archiefnominatie? = null,
    opschorting: Opschorting? = null,
    einddatumGepland: LocalDate? = null,
    identificatie: String = "dummyIdentificatie",
    registratiedatum: LocalDate = LocalDate.now(),
    resultaat: URI? = null,
    uiterlijkeEinddatumAfdoening: LocalDate = LocalDate.now().plusDays(1),
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    status: URI? = null,
    verlenging: Verlenging? = null,
    deelzaken: Set<URI>? = null,
    uuid: UUID = UUID.randomUUID(),
    omschrijving: String = "dummyOmschrijving"
) = Zaak(
    zaakTypeURI,
    startDate,
    bronOrganisatie,
    verantwoordelijkeOrganisatie
).apply {
    this.url = URI("https://example.com/zaak/${UUID.randomUUID()}")
    this.uuid = uuid
    this.archiefnominatie = archiefnominatie
    this.opschorting = opschorting
    this.einddatumGepland = einddatumGepland
    this.identificatie = identificatie
    this.registratiedatum = registratiedatum
    this.resultaat = resultaat
    this.uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.status = status
    this.verlenging = verlenging
    this.deelzaken = deelzaken
    this.omschrijving = omschrijving
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

fun createZaakInformatieobject(
    informatieobjectUUID: UUID = UUID.randomUUID(),
    zaakUUID: UUID = UUID.randomUUID(),
    informatieObjectURL: URI = URI("http://example.com/$informatieobjectUUID"),
    zaakURL: URI = URI("http://example.com/$zaakUUID")
) = ZaakInformatieobject(
    informatieObjectURL,
    zaakURL
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
    reden: String = "dummyReden",
    duur: Period = Period.ZERO
) = Verlenging(reden, duur)
