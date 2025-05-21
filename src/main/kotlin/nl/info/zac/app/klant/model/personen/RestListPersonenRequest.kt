/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.personen

data class RestListPersonenRequest(
    val persoon: RestListPersonenParameters,
    val context: RestContext
)
