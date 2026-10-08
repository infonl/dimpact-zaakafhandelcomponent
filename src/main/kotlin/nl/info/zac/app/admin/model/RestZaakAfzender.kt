/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.admin.model.ZaaktypeZaakafzenderParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * A REST representation of a ZaakAfzender.
 * Note that all fields in this class need to be vars and not vals because of the way data value classes
 * are instantiated in the ZAC REST API using JAX-RS.
 */
@NoArgConstructor
@AllOpen
data class RestZaakAfzender(
    var id: Long? = null,

    @get:JsonbProperty("isDefaultMail")
    @set:JsonbProperty("isDefaultMail")
    var isDefaultMail: Boolean = false,
    // should be non-nullable but for now leave it nullable because making
    // any of these field non-nullable currently breaks the ZAC frontend code build ('Conversion of type' errors)
    var mail: String? = null,
    var suffix: String? = null,
    var replyTo: String? = null,

    @get:JsonbProperty("isSpeciaal")
    @set:JsonbProperty("isSpeciaal")
    var isSpeciaal: Boolean = false
)

fun RestZaakAfzender.toZaakAfzender() = ZaaktypeZaakafzenderParameters().apply {
    id = this@toZaakAfzender.id
    isDefaultMail = this@toZaakAfzender.isDefaultMail
    this@toZaakAfzender.mail?.let { mail = it }
    this@toZaakAfzender.replyTo?.let { replyTo = it }
}

fun ZaaktypeZaakafzenderParameters.toRestZaakAfzender() = RestZaakAfzender(
    id = this@toRestZaakAfzender.id,
    isDefaultMail = this@toRestZaakAfzender.isDefaultMail,
    mail = this@toRestZaakAfzender.mail,
    replyTo = this@toRestZaakAfzender.replyTo,
    isSpeciaal = ZaaktypeZaakafzenderParameters.SpecialMail.entries.any { it.name(this@toRestZaakAfzender.mail) },
    suffix = null
)

fun List<RestZaakAfzender>.toZaakAfzenders(): List<ZaaktypeZaakafzenderParameters> =
    this@toZaakAfzenders.filter { !it.isSpeciaal || it.isDefaultMail || it.replyTo != null }
        .map { it.toZaakAfzender() }

fun Set<ZaaktypeZaakafzenderParameters>.toRestZaakAfzenders(): List<RestZaakAfzender> {
    val restZaakAfzenders = this@toRestZaakAfzenders.map { it.toRestZaakAfzender() }.toMutableList()
    // now add the 'special' zaakafzender emails, if they are not already present
    for (speciaal in ZaaktypeZaakafzenderParameters.SpecialMail.entries) {
        if (this@toRestZaakAfzenders.map { it.mail }.none { speciaal.name == it }) {
            restZaakAfzenders.add(
                RestZaakAfzender(
                    mail = speciaal.name,
                    isSpeciaal = true,
                )
            )
        }
    }
    return restZaakAfzenders
}
