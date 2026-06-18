/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.OverigeRechten

data class RestOverigeRechten(
    val startenZaak: Boolean,
    val beheren: Boolean,
    val zoeken: Boolean
)

fun OverigeRechten.toRestOverigeRechten() = RestOverigeRechten(
    startenZaak = this.startenZaak,
    beheren = this.beheren,
    zoeken = this.zoeken
)
