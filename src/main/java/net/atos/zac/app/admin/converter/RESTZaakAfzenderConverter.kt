/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.ZaakAfzender
import nl.info.zac.app.admin.model.RestZaakAfzender
import nl.info.zac.app.admin.model.toZaakAfzender

fun convertZaakAfzenders(zaakAfzender: Set<ZaakAfzender>): List<RestZaakAfzender> {
    val restZaakAfzenders = zaakAfzender.map { convertZaakAfzender(it) }.toMutableList()
    for (speciaal in ZaakAfzender.Speciaal.entries) {
        if (zaakAfzender.map { it.mail }.none { speciaal.`is`(it) }) {
            restZaakAfzenders.add(RestZaakAfzender(speciaal))
        }
    }
    return restZaakAfzenders
}

fun convertRESTZaakAfzenders(restZaakAfzender: List<RestZaakAfzender>): List<ZaakAfzender> =
    restZaakAfzender
        .filter { !it.speciaal || it.defaultMail || it.replyTo != null }
        .map { it.toZaakAfzender() }

fun convertZaakAfzender(zaakAfzender: ZaakAfzender) = RestZaakAfzender(
    id = zaakAfzender.id,
    defaultMail = zaakAfzender.isDefault,
    mail = zaakAfzender.mail,
    replyTo = zaakAfzender.replyTo,
    speciaal = ZaakAfzender.Speciaal.entries.any { it.`is`(zaakAfzender.mail) }
)
