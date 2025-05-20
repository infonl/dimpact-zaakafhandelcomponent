/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.model

data class RestWerklijstRechten(
    val inbox: Boolean,
    val ontkoppeldeDocumentenVerwijderen: Boolean,
    val inboxProductaanvragenVerwijderen: Boolean,
    val zakenTaken: Boolean,
    val zakenTakenVerdelen: Boolean,
    val zakenTakenExporteren: Boolean
)
