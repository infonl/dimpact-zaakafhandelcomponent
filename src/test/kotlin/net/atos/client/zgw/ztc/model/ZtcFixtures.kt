package net.atos.client.zgw.ztc.model

import net.atos.client.zgw.shared.model.Vertrouwelijkheidaanduiding
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

fun createZaakType() = Zaaktype(
    URI("http://example.com/zaaktypes/${UUID.randomUUID()}"),
    setOf(URI("dummyStatusType1"), URI("dummyStatusType2")),
    setOf(URI("dummyResultaatType1"), URI("dummyResultaatType2")),
    setOf(URI("dummyEigenschap1"), URI("dummyEigenschap2")),
    setOf(URI("dummyInformatieObjectType1"), URI("dummyInformatieObjectType2")),
    setOf(URI("dummyRolType1"), URI("dummyRolType2")),
    false
)

fun createInformatieObjectType(
    catalogusURI: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    omschrijving: String = "dummyOmschrijving",
    vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding = Vertrouwelijkheidaanduiding.OPENBAAR,
    beginGeldigheid: LocalDate = LocalDate.now()
) = Informatieobjecttype(
    catalogusURI,
    omschrijving,
    vertrouwelijkheidaanduiding,
    beginGeldigheid
)
