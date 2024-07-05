package net.atos.zac.app.informatieobjecten.model

import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createRESTEnkelvoudigInformatieobject(
    uuid: UUID = UUID.randomUUID(),
    status: StatusEnum = StatusEnum.IN_BEWERKING,
    vertrouwelijkheidaanduiding: String? = null,
    creatieDatum: LocalDate? = null,
    auteur: String? = null,
    taal: String? = null,
    informatieobjectTypeUUID: UUID = UUID.randomUUID(),
    file: ByteArray = "dummyFile".toByteArray(),
    bestandsNaam: String = "dummyFilename",
    formaat: String = "dummyType",
    indicatieGebruiksrecht: Boolean? = null
) = RESTEnkelvoudigInformatieobject().apply {
    this.uuid = uuid
    this.status = status
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.creatiedatum = creatieDatum
    this.auteur = auteur
    this.taal = taal
    this.informatieobjectTypeUUID = informatieobjectTypeUUID
    this.file = file
    this.bestandsnaam = bestandsNaam
    this.formaat = formaat
    this.indicatieGebruiksrecht = indicatieGebruiksrecht ?: false
}

fun createRESTFileUpload(
    file: ByteArray = "dummyFile".toByteArray(),
    fileSize: Long = 123L,
    filename: String = "dummyFilename",
    type: String = "dummyType"
) = RESTFileUpload().apply {
    this.file = file
    this.filename = filename
    this.fileSize = fileSize
    this.type = type
}

fun createRESTInformatieobjecttype(
    uuid: UUID = UUID.randomUUID(),
    omschrijving: String = "dummyOmschrijving",
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.name,
    concept: Boolean = false
) = RESTInformatieobjecttype().apply {
    this.uuid = uuid
    this.omschrijving = omschrijving
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.concept = concept
}

@Suppress("LongParameterList")
fun createRESTEnkelvoudigInformatieObjectVersieGegevens(
    uuid: UUID = UUID.randomUUID(),
    zaakUuid: UUID = UUID.randomUUID(),
    bestandsnaam: String = "dummyFile.txt",
    file: ByteArray = "dummyFile".toByteArray(),
    formaat: String = "dummyType",
    informatieobjectTypeUUID: UUID = UUID.randomUUID(),
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.name
) = RESTEnkelvoudigInformatieObjectVersieGegevens().apply {
    this.uuid = uuid
    this.zaakUuid = zaakUuid
    this.bestandsnaam = bestandsnaam
    this.formaat = formaat
    this.file = file
    this.informatieobjectTypeUUID = informatieobjectTypeUUID
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
}

fun createRESTInformatieobjectZoekParameters(
    informatieobjectUUIDs: List<UUID>? = listOf(UUID.randomUUID(), UUID.randomUUID()),
    zaakUuid: UUID = UUID.randomUUID(),
    besluittypeUuid: UUID = UUID.randomUUID(),
    gekoppeldeZaakDocumenten: Boolean = false
) = RESTInformatieobjectZoekParameters().apply {
    this.informatieobjectUUIDs = informatieobjectUUIDs
    this.zaakUUID = zaakUuid
    this.besluittypeUUID = besluittypeUuid
    this.gekoppeldeZaakDocumenten = gekoppeldeZaakDocumenten
}
