/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.shared

import jakarta.ws.rs.QueryParam
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestListParameters {
    @field:QueryParam("sort")
    var sort: String? = null

    @field:QueryParam("order")
    var order: String? = null

    @field:QueryParam("page")
    var page: Int = 0

    @field:QueryParam("maxResults")
    var maxResults: Int = 0
}
