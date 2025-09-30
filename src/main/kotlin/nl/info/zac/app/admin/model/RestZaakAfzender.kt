/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters
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
    var defaultMail: Boolean = false,
    // should be non-nullable but for now leave it nullable because making
    // any of these field non-nullable currently breaks the ZAC frontend code build ('Conversion of type' errors)
    var mail: String? = null,
    var suffix: String? = null,
    var replyTo: String? = null,
    var speciaal: Boolean = false
)

fun RestZaakAfzender.toZaakAfzender() = ZaaktypeCmmnZaakafzenderParameters().apply {
    id = this@toZaakAfzender.id
    defaultMail = this@toZaakAfzender.defaultMail
    this@toZaakAfzender.mail?.let { mail = it }
    replyTo = this@toZaakAfzender.replyTo
}

fun ZaaktypeCmmnZaakafzenderParameters.toRestZaakAfzender() = RestZaakAfzender(
    id = this@toRestZaakAfzender.id,
    defaultMail = this@toRestZaakAfzender.defaultMail,
    mail = this@toRestZaakAfzender.mail,
    replyTo = this@toRestZaakAfzender.replyTo,
    speciaal = ZaaktypeCmmnZaakafzenderParameters.SpecialMail.entries.any { it.name(this@toRestZaakAfzender.mail) },
    suffix = null
)

fun List<RestZaakAfzender>.toZaakAfzenders(): List<ZaaktypeCmmnZaakafzenderParameters> =
    this@toZaakAfzenders.filter { !it.speciaal || it.defaultMail || it.replyTo != null }
        .map { it.toZaakAfzender() }

fun Set<ZaaktypeCmmnZaakafzenderParameters>.toRestZaakAfzenders(): List<RestZaakAfzender> {
    val restZaakAfzenders = this@toRestZaakAfzenders.map { it.toRestZaakAfzender() }.toMutableList()
    // now add the 'special' zaakafzender emails, if they are not already present
    for (speciaal in ZaaktypeCmmnZaakafzenderParameters.SpecialMail.entries) {
        if (this@toRestZaakAfzenders.map { it.mail }.none { speciaal.name == it }) {
            restZaakAfzenders.add(
                RestZaakAfzender(
                    mail = speciaal.name,
                    speciaal = true,
                )
            )
        }
    }
    return restZaakAfzenders
}
