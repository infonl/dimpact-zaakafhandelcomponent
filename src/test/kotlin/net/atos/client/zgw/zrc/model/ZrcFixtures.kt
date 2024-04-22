package net.atos.client.zgw.zrc.model

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.model.generated.Opschorting
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.generated.RolType
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

fun createRolMedewerker(
    zaak: URI = URI("http://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(),
    roltoelichting: String = "dummyToelichting",
    betrokkeneIdentificatie: Medewerker = createMedewerker()
) = RolMedewerker(
    zaak,
    rolType,
    roltoelichting,
    betrokkeneIdentificatie
)

fun createRolNatuurlijkPersoon(
    zaaktypeURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(zaaktypeURI),
    toelichting: String = "dummyToelichting",
    natuurlijkPersoon: NatuurlijkPersoon = createNatuurlijkPersoon()
) = RolNatuurlijkPersoon(
    zaaktypeURI,
    rolType,
    toelichting,
    natuurlijkPersoon
)

fun createRolOrganisatorischeEenheid(
    zaaktypeURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    rolType: RolType = createRolType(zaaktypeURI),
    toelichting: String = "dummyToelichting",
    organisatorischeEenheid: OrganisatorischeEenheid = createOrganisatorischeEenheid()
) = RolOrganisatorischeEenheid(
    zaaktypeURI,
    rolType,
    toelichting,
    organisatorischeEenheid
)

@Suppress("LongParameterList")
fun createZaak(
    zaaktypeURI: URI = URI("https://example.com/${UUID.randomUUID()}"),
    startDate: LocalDate = LocalDate.now(),
    bronOrganisatie: String = "dummyBronOrganisatie",
    verantwoordelijkeOrganisatie: String = "dummyVerantwoordelijkeOrganisatie",
    // an archiefnominatie which is not null means that the zaak is closed
    archiefnominatie: Archiefnominatie? = null,
    opschorting: Opschorting? = null,
    einddatumGepland: LocalDate? = null,
    registratiedatum: LocalDate = LocalDate.now(),
    uiterlijkeEinddatumAfdoening: LocalDate = LocalDate.now().plusDays(1),
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    status: URI? = URI("https://example.com/${UUID.randomUUID()}"),
    verlenging: Verlenging? = null
) = Zaak(
    zaaktypeURI,
    startDate,
    bronOrganisatie,
    verantwoordelijkeOrganisatie
).apply {
    this.url = URI("https://example.com/zaak/${UUID.randomUUID()}")
    this.uuid = UUID.randomUUID()
    this.archiefnominatie = archiefnominatie
    this.opschorting = opschorting
    this.einddatumGepland = einddatumGepland
    this.registratiedatum = registratiedatum
    this.uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.status = status
    this.verlenging = verlenging
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
    informatieObjectURL: URI = URI("http://example.com/${UUID.randomUUID()}"),
    zaakURL: URI = URI("http://example.com/${UUID.randomUUID()}")
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
