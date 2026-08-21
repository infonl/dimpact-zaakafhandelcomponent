/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.model

import jakarta.validation.constraints.NotBlank
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.util.NoArgConstructor

/**
 * REST gegevens voor het verzenden van mail
 */
@NoArgConstructor
data class RESTMailGegevens(
    @field:NotBlank
    var verzender: String,

    @field:NotBlank
    var ontvanger: String,

    @field:NotBlank
    var onderwerp: String,

    @field:NotBlank
    var body: String,

    var replyTo: String? = null,

    var bijlagen: String? = null,

    var createDocumentFromMail: Boolean = false,

    var vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = null
)
