/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.WerklijstRechten

data class RestWerklijstRechten(
    val inbox: Boolean,
    val ontkoppeldeDocumentenVerwijderen: Boolean,
    val inboxProductaanvragenVerwijderen: Boolean,
    val zakenTaken: Boolean,
    val zakenTakenVerdelen: Boolean,
    val zakenTakenExporteren: Boolean
)

fun WerklijstRechten.toRestWerklijstRechten() = RestWerklijstRechten(
    inbox = this.inbox,
    ontkoppeldeDocumentenVerwijderen = this.ontkoppeldeDocumentenVerwijderen,
    inboxProductaanvragenVerwijderen = this.inboxProductaanvragenVerwijderen,
    zakenTaken = this.zakenTaken,
    zakenTakenVerdelen = this.zakenTakenVerdelen,
    zakenTakenExporteren = this.zakenTakenExporteren
)
