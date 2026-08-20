/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.inboxdocument.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestInboxDocumentDeleteResult(
    @get:JsonbProperty("isGekoppeldAanZaak")
    var isGekoppeldAanZaak: Boolean = false
)
