/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * REST gegevens voor het verzenden van mail
 */
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
data class RestMailGegevens(
    @field:NotBlank
    var verzender: String,

    @field:NotBlank
    var ontvanger: String,

    var replyTo: String? = null,

    @field:NotBlank
    var onderwerp: String,

    @field:NotBlank
    var body: String,

    var bijlagen: String? = null,

    var createDocumentFromMail: Boolean = false,

    @field:NotNull
    var vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding
)
