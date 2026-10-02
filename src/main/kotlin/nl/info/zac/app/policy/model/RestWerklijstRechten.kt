/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.WerklijstRechten

data class RestWerklijstRechten(
    val canInbox: Boolean,
    val canOntkoppeldeDocumentenVerwijderen: Boolean,
    val canInboxProductaanvragenVerwijderen: Boolean,
    val canZakenTaken: Boolean,
    val canZakenTakenVerdelen: Boolean,
    val canZakenTakenExporteren: Boolean
)

fun WerklijstRechten.toRestWerklijstRechten() = RestWerklijstRechten(
    canInbox = this.canInbox,
    canOntkoppeldeDocumentenVerwijderen = this.canOntkoppeldeDocumentenVerwijderen,
    canInboxProductaanvragenVerwijderen = this.canInboxProductaanvragenVerwijderen,
    canZakenTaken = this.canZakenTaken,
    canZakenTakenVerdelen = this.canZakenTakenVerdelen,
    canZakenTakenExporteren = this.canZakenTakenExporteren
)
