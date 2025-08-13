/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.ZaakAfzender
import nl.info.zac.app.admin.model.RestZaakAfzender
import nl.info.zac.app.admin.model.toRestZaakAfzender
import nl.info.zac.app.admin.model.toZaakAfzender

fun convertZaakAfzenders(zaakAfzender: Set<ZaakAfzender>): List<RestZaakAfzender> {
    val restZaakAfzenders = zaakAfzender.map { it.toRestZaakAfzender() }.toMutableList()
    for (speciaal in ZaakAfzender.Speciaal.entries) {
        if (zaakAfzender.map { it.mail }.none { speciaal.`is`(it) }) {
            restZaakAfzenders.add(RestZaakAfzender(speciaal))
        }
    }
    return restZaakAfzenders
}

