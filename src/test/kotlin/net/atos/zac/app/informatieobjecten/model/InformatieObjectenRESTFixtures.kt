package net.atos.zac.app.informatieobjecten.model

import net.atos.client.zgw.ztc.model.generated.ZaakType.VertrouwelijkheidaanduidingEnum
import java.util.*

fun createRESTEnkelvoudigInformatieobject(
    uuid: UUID = UUID.randomUUID(),
) = RESTEnkelvoudigInformatieobject().apply {
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
