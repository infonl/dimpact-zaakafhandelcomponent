/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail.model

import jakarta.json.bind.annotation.JsonbProperty

data class Attachment(
    @field:JsonbProperty("ContentType") var contentType: String,
    @field:JsonbProperty("Filename") var filename: String,
    @field:JsonbProperty("Base64Content") var base64Content: String
)
