/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren.model

import nl.info.zac.app.policy.model.RestWerklijstRechten

data class RestTabelGegevens(
    val aantalPerPagina: Int,
    val pageSizeOptions: List<Int>,
    val werklijstRechten: RestWerklijstRechten
)
