/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.model

import java.time.LocalDate

data class RestOndertekening(
    var soort: String,
    var datum: LocalDate
)
