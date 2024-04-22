package net.atos.client.zgw.ztc.model

import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.StatusType
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
    omschrijving: String = "dummyZaakTypeOmschrijving",
    informatieObjectTypen: Set<URI>? = setOf(URI("dummyInformatieObjectType1"), URI("dummyInformatieObjectType2")),
    besluittypen: Set<URI>? = null,
) = ZaakType(
    URI("http://example.com/zaaktypes/$uuid"),
    false,
    null,
    null,
    setOf(URI("dummyStatusType1"), URI("dummyStatusType2")),
    setOf(URI("dummyResultaatType1"), URI("dummyResultaatType2")),
    setOf(URI("dummyEigenschap1"), URI("dummyEigenschap2")),
    informatieObjectTypen,
    setOf(URI("dummyRolType1"), URI("dummyRolType2")),
    null
).apply {
    this.omschrijving = omschrijving
    this.besluittypen = besluittypen
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

@Suppress("LongParameterList")
fun createStatusType(
    uri: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    zaaktypeIdentificatie: String? = null,
    isEindstatus: Boolean = false,
    catalogus: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    eigenschappen: Set<URI> = setOf(URI("http://example.com/catalogus/${UUID.randomUUID()}")),
    zaakobjecttypen: Set<URI> = setOf(URI("http://example.com/catalogus/${UUID.randomUUID()}")),
    beginObject: LocalDate = LocalDate.now(),
    eindeObject: LocalDate = LocalDate.now(),
    omschrijving: String? = null
) = StatusType(
    uri,
    zaaktypeIdentificatie,
    isEindstatus,
    catalogus,
    eigenschappen,
    zaakobjecttypen,
    beginObject,
    eindeObject
).apply {
    this.omschrijving = omschrijving
}
