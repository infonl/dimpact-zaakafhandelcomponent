package net.atos.client.zgw.ztc.model

import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createRolType(
    zaakTypeURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    omschrijving: String = "dummyOmschrijving",
    omschrijvingGeneriek: RolType.OmschrijvingGeneriekEnum = RolType.OmschrijvingGeneriekEnum.INITIATOR
) = RolType().apply {
    this.zaaktype = zaakTypeURI
    this.omschrijving = omschrijving
    this.omschrijvingGeneriek = omschrijvingGeneriek
}

fun createZaakType(
    uuid: UUID = UUID.randomUUID(),
    omschrijving: String = "dummyZaakTypeOmschrijving"
) = ZaakType(
    URI("http://example.com/zaaktypes/$uuid"),
    false,
    null,
    null,
    setOf(URI("dummyStatusType1"), URI("dummyStatusType2")),
    setOf(URI("dummyResultaatType1"), URI("dummyResultaatType2")),
    setOf(URI("dummyEigenschap1"), URI("dummyEigenschap2")),
    setOf(URI("dummyInformatieObjectType1"), URI("dummyInformatieObjectType2")),
    setOf(URI("dummyRolType1"), URI("dummyRolType2")),
    null
).apply {
    this.omschrijving = omschrijving
}

fun createInformatieObjectType(
    catalogusURI: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    omschrijving: String = "dummyOmschrijving",
    vertrouwelijkheidaanduiding: InformatieObjectType.VertrouwelijkheidaanduidingEnum =
        InformatieObjectType.VertrouwelijkheidaanduidingEnum.OPENBAAR,
    beginGeldigheid: LocalDate = LocalDate.now()
) = InformatieObjectType(
    catalogusURI,
    false,
    null,
    null,
    null,
    null
).apply {
    this.omschrijving = omschrijving
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.beginGeldigheid = beginGeldigheid
}
