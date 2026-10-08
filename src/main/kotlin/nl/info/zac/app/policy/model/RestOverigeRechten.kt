/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.OverigeRechten

data class RestOverigeRechten(
    val canStartenZaak: Boolean,
    val canBeheren: Boolean,
    val canZoeken: Boolean
)

fun OverigeRechten.toRestOverigeRechten() = RestOverigeRechten(
    canStartenZaak = this.canStartenZaak,
    canBeheren = this.canBeheren,
    canZoeken = this.canZoeken
)
