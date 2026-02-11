/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.configuration.model

import nl.info.zac.configuration.model.Taal

fun createTaal(
    id: Long,
    code: String,
    naam: String,
    name: String,
    local: String
) = Taal().apply {
    this.id = id
    this.code = code
    this.naam = naam
    this.name = name
    this.local = local
}
