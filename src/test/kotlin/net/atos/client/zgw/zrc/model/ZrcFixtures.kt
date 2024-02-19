package net.atos.client.zgw.zrc.model

import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.generated.RolType
import java.net.URI
import java.time.LocalDate
import java.util.UUID

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

fun createZaak(
    zaaktypeURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    startDate: LocalDate = LocalDate.now(),
    bronOrganisatie: String = "dummyBronOrganisatie",
    verantwoordelijkeOrganisatie: String = "dummyVerantwoordelijkeOrganisatie",
    // an archiefnominatie which is not null means that the zaak is closed
    archiefnominatie: Archiefnominatie? = null
) = Zaak(
    zaaktypeURI,
    startDate,
    bronOrganisatie,
    verantwoordelijkeOrganisatie
).apply {
    this.url = URI("https://example.com/zaak/${UUID.randomUUID()}")
    this.uuid = UUID.randomUUID()
    this.archiefnominatie = archiefnominatie
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
