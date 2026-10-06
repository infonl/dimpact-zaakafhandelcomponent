/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters.SpecialMail
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestReplyTo(
    var mail: String = "",

    @get:JsonbProperty("isSpeciaal")
    @set:JsonbProperty("isSpeciaal")
    var isSpeciaal: Boolean = false
)

fun ReferenceTableValue.toRestReplyTo() = RestReplyTo(mail = name, isSpeciaal = false)

fun SpecialMail.toRestReplyTo() = RestReplyTo(mail = name, isSpeciaal = true)

/**
 * Returns the given reference table values and every [SpecialMail] as reply-tos, with the special ones first and the
 * others ordered by mail address.
 */
fun List<ReferenceTableValue>.toRestReplyTos(): List<RestReplyTo> =
    (map { it.toRestReplyTo() } + SpecialMail.entries.map { it.toRestReplyTo() })
        .sortedWith(compareByDescending<RestReplyTo> { it.isSpeciaal }.thenBy { it.mail })
