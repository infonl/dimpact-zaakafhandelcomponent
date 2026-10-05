/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters.SpecialMail
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

// The class keeps its Java-era name, because the name is the OpenAPI schema name that the frontend uses.
@NoArgConstructor
@AllOpen
data class RESTReplyTo(
    var mail: String = "",
    var speciaal: Boolean = false
)

fun ReferenceTableValue.toRestReplyTo() = RESTReplyTo(mail = name, speciaal = false)

fun SpecialMail.toRestReplyTo() = RESTReplyTo(mail = name, speciaal = true)

/**
 * Returns the given reference table values and every [SpecialMail] as reply-tos, with the special ones first and the
 * others ordered by mail address.
 */
fun List<ReferenceTableValue>.toRestReplyTos(): List<RESTReplyTo> =
    (map { it.toRestReplyTo() } + SpecialMail.entries.map { it.toRestReplyTo() })
        .sortedWith(compareByDescending<RESTReplyTo> { it.speciaal }.thenBy { it.mail })
