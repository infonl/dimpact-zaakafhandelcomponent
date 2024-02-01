package net.atos.client.zgw.ztc.model

import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObject
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createRolType(
    zaakTypeURI: URI = URI("http://example.com/${UUID.randomUUID()}"),
    omschrijving: String = "dummyOmschrijving",
    rol: AardVanRol = AardVanRol.INITIATOR
) = Roltype(
    zaakTypeURI,
    omschrijving,
    rol
)

fun createZaakType(
    uuid: UUID = UUID.randomUUID(),
    omschrijving: String = "dummyZaakTypeOmschrijving"
) = Zaaktype(
    URI("http://example.com/zaaktypes/$uuid"),
    setOf(URI("dummyStatusType1"), URI("dummyStatusType2")),
    setOf(URI("dummyResultaatType1"), URI("dummyResultaatType2")),
    setOf(URI("dummyEigenschap1"), URI("dummyEigenschap2")),
    setOf(URI("dummyInformatieObjectType1"), URI("dummyInformatieObjectType2")),
    setOf(URI("dummyRolType1"), URI("dummyRolType2")),
    false
).apply {
    this.omschrijving = omschrijving
}

fun createInformatieObjectType(
    catalogusURI: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    omschrijving: String = "dummyOmschrijving",
    vertrouwelijkheidaanduiding: EnkelvoudigInformatieObject.VertrouwelijkheidaanduidingEnum =
        EnkelvoudigInformatieObject.VertrouwelijkheidaanduidingEnum.OPENBAAR,
    beginGeldigheid: LocalDate = LocalDate.now()
) = Informatieobjecttype(
    catalogusURI,
    omschrijving,
    vertrouwelijkheidaanduiding,
    beginGeldigheid
)
