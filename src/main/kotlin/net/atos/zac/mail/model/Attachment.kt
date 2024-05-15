package net.atos.zac.mail.model

import jakarta.json.bind.annotation.JsonbProperty

class Attachment(
    @field:JsonbProperty("ContentType") var contentType: String,
    @field:JsonbProperty("Filename") var filename: String,
    @field:JsonbProperty("Base64Content") var base64Content: String
)
