/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.model

import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.util.NoArgConstructor

/**
 * REST gegevens voor het verzenden van mail
 */
@NoArgConstructor
data class RestMailGegevens(
    var verzender: String? = null,

    var ontvanger: String? = null,

    var onderwerp: String? = null,

    var body: String? = null,

    var replyTo: String? = null,

    var bijlagen: String? = null,

    var createDocumentFromMail: Boolean = false,

    var vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = null
)
