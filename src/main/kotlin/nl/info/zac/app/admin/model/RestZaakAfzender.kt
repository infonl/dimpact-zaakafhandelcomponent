/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import net.atos.zac.admin.model.ZaakAfzender
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
open class RestZaakAfzender(
    val id: Long? = null,
    val defaultMail: Boolean = false,
    // should probably be non-nullable but for now leave it nullable because making
    // any of these field non-nullable currently breaks the ZAC frontend code build ('Conversion of type' errors)
    val mail: String? = null,
    val suffix: String? = null,
    val replyTo: String? = null,
    val speciaal: Boolean = false
)

fun RestZaakAfzender.toZaakAfzender() = ZaakAfzender().apply {
    id = this@toZaakAfzender.id
    isDefault = this@toZaakAfzender.defaultMail
    mail = this@toZaakAfzender.mail
    replyTo = this@toZaakAfzender.replyTo
}

fun ZaakAfzender.toRestZaakAfzender() = RestZaakAfzender(
    id = this@toRestZaakAfzender.id,
    defaultMail = this@toRestZaakAfzender.isDefault,
    mail = this@toRestZaakAfzender.mail,
    replyTo = this@toRestZaakAfzender.replyTo,
    speciaal = ZaakAfzender.SpecialMail.entries.any { it.`is`(this@toRestZaakAfzender.mail) },
    suffix = null
)

fun List<RestZaakAfzender>.toZaakAfzenders(): List<ZaakAfzender> =
    this@toZaakAfzenders.filter { !it.speciaal || it.defaultMail || it.replyTo != null }
        .map { it.toZaakAfzender() }

fun Set<ZaakAfzender>.toRestZaakAfzenders(): List<RestZaakAfzender> {
    val restZaakAfzenders = this@toRestZaakAfzenders.map { it.toRestZaakAfzender() }.toMutableList()
    // now add the 'special' zaakafzender emails, if they are not already present
    for (speciaal in ZaakAfzender.SpecialMail.entries) {
        if (this@toRestZaakAfzenders.map { it.mail }.none { speciaal.`is`(it) }) {
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
