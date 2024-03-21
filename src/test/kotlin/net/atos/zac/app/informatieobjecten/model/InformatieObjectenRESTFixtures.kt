package net.atos.zac.app.informatieobjecten.model

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject.StatusEnum
import net.atos.client.zgw.ztc.model.generated.ZaakType.VertrouwelijkheidaanduidingEnum
import java.time.LocalDate
import java.util.*

@Suppress("LongParameterList")
fun createRESTEnkelvoudigInformatieobject(
    uuid: UUID = UUID.randomUUID(),
    status: StatusEnum = StatusEnum.IN_BEWERKING,
    vertrouwelijkheidaanduiding: String? = null,
    creatieDatum: LocalDate? = null,
    auteur: String? = null,
    taal: String? = null,
    bestandsNaam: String? = null,
    informatieobjectTypeUUID: UUID = UUID.randomUUID()
) = RESTEnkelvoudigInformatieobject().apply {
    this.uuid = uuid
    this.status = status
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.creatiedatum = creatieDatum
    this.auteur = auteur
    this.taal = taal
    this.bestandsnaam = bestandsNaam
    this.informatieobjectTypeUUID = informatieobjectTypeUUID
}

fun createRESTEnkelvoudigInformatieObjectVersieGegevens(
    uuid: UUID = UUID.randomUUID(),
) = RESTEnkelvoudigInformatieObjectVersieGegevens().apply {
    this.uuid = uuid
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
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.value().uppercase(),
    concept: Boolean = false
) = RESTInformatieobjecttype().apply {
    this.uuid = uuid
    this.omschrijving = omschrijving
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.concept = concept
}

fun createRESTEnkelvoudigInformatieObjectVersieGegevens(
    uuid: UUID = UUID.randomUUID(),
    zaakUuid: UUID
) = RESTEnkelvoudigInformatieObjectVersieGegevens().apply {
    this.uuid = uuid
    this.zaakUuid = zaakUuid
}
