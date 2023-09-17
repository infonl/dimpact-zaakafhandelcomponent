package net.atos.client.zgw.zrc.model

import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.client.zgw.ztc.model.Roltype
import net.atos.client.zgw.ztc.model.createRolType
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createNatuurlijkPersoon(bsn: String = "dummyBsn") = NatuurlijkPersoon(bsn)

fun createObjectPand(identificatie: String = "dummyIdentificatie") = ObjectPand(identificatie)

fun createRolNatuurlijkPersoon(
    zaaktypeURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    rolType: Roltype = createRolType(zaaktypeURI),
    toelichting: String = "dummyToelichting",
    natuurlijkPersoon: NatuurlijkPersoon = createNatuurlijkPersoon()
) = RolNatuurlijkPersoon(
    zaaktypeURI,
    rolType,
    toelichting,
    natuurlijkPersoon
)

fun createZaak(zaaktypeURI: URI = URI("http://example.com/${UUID.randomUUID()}}")) =
    Zaak(
        zaaktypeURI,
        LocalDate.now(),
        "dummyBronOrganisatie",
        "dummyVerantwoordelijkeOrganisatie"
    ).apply {
        url = URI("https://example.com/zaak/${UUID.randomUUID()}")
        uuid = UUID.randomUUID()
    }

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
