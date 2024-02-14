package net.atos.zac.app.informatieobjecten.model

import java.util.UUID

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
