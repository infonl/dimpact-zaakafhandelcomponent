/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.ZaakAfzender
import net.atos.zac.app.admin.model.RESTZaakAfzender

fun convertZaakAfzenders(zaakAfzender: Set<ZaakAfzender>): List<RESTZaakAfzender> {
    val restZaakAfzenders = zaakAfzender.map { convertZaakAfzender(it) }.toMutableList()
    for (speciaal in ZaakAfzender.Speciaal.entries) {
        if (zaakAfzender.map { it.mail }.none { speciaal.`is`(it) }) {
            restZaakAfzenders.add(RESTZaakAfzender(speciaal))
        }
    }
    return restZaakAfzenders
}

fun convertRESTZaakAfzenders(restZaakAfzender: List<RESTZaakAfzender>): List<ZaakAfzender> =
    restZaakAfzender
        .filter { !it.speciaal || it.defaultMail || it.replyTo != null }
        .map { convertRESTZaakAfzender(it) }

fun convertZaakAfzender(zaakAfzender: ZaakAfzender) = RESTZaakAfzender(
    id = zaakAfzender.id,
    defaultMail = zaakAfzender.isDefault,
    mail = zaakAfzender.mail,
    replyTo = zaakAfzender.replyTo,
    speciaal = ZaakAfzender.Speciaal.entries.any { it.`is`(zaakAfzender.mail) }
)

fun convertRESTZaakAfzender(restZaakAfzender: RESTZaakAfzender) = ZaakAfzender().apply {
    id = restZaakAfzender.id
    isDefault = restZaakAfzender.defaultMail
    mail = restZaakAfzender.mail
    replyTo = restZaakAfzender.replyTo
}
