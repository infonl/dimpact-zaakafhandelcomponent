/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model

import nl.info.client.zgw.drc.model.generated.Ondertekening
import java.time.LocalDate
import java.util.Locale

data class RestOndertekening(
    var soort: String,
    var datum: LocalDate
)

fun Ondertekening.toRestOndertekening() = RestOndertekening(
    soort = this@toRestOndertekening.getSoort().name.lowercase(Locale.getDefault()),
    datum = this@toRestOndertekening.getDatum()
)
