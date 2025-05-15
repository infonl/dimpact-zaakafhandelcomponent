/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.contactmoment

data class RestListContactmomentenParameters(
    var bsn: String? = null,
    var vestigingsnummer: String? = null,
    var page: Int? = null
)
