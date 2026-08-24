/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.ws.rs.QueryParam

abstract class AbstractListParameters {
    /**
     * Een pagina binnen de gepagineerde set resultaten.
     */
    @QueryParam("page")
    var page: Int? = null
}
