package net.atos.client.zgw.zrc.model

import net.atos.client.zgw.ztc.model.Roltype
import net.atos.client.zgw.ztc.model.createRolType
import java.net.URI
import java.time.LocalDate

fun createNatuurlijkPersoon() = NatuurlijkPersoon("dummyBsn")

fun createRolNatuurlijkPersoon(zaaktypeURI: URI, natuurlijkPersoon: NatuurlijkPersoon) = RolNatuurlijkPersoon(
    URI("dummyZaakUri"),
    createRolType(zaaktypeURI),
    "dummyToelichting",
    natuurlijkPersoon
)

fun createZaak(zaaktypeURI: URI) = Zaak(
    zaaktypeURI,
    LocalDate.now(),
    "dummyBronOrganisatie",
    "dummyVerantwoordelijkeOrganisatie"
)
