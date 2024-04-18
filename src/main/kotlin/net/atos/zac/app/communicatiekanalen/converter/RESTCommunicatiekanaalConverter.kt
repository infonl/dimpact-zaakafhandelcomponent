/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.communicatiekanalen.converter

import jakarta.inject.Inject
import jakarta.ws.rs.core.UriInfo
import net.atos.zac.app.communicatiekanalen.model.RESTCommunicatiekanaal
import net.atos.zac.zaaksturing.model.ReferentieTabelWaarde
import java.net.URI

class RestCommunicatiekanaalConverter {
    @Inject
    lateinit var uriInfo: UriInfo

    fun convertToRESTCommunicatiekanaal(communicatieKanaal: ReferentieTabelWaarde): RESTCommunicatiekanaal =
        RESTCommunicatiekanaal(
            url = createUriFromCommunicatieKanaalId(communicatieKanaal.id),
            naam = communicatieKanaal.naam,
            omschrijving = communicatieKanaal.naam
        )

    fun convertToRESTCommunicatiekanalen(
        communicatieKanalen: List<ReferentieTabelWaarde>
    ): List<RESTCommunicatiekanaal> = communicatieKanalen.stream()
        .map { convertToRESTCommunicatiekanaal(it) }
        .toList()

    fun createUriFromCommunicatieKanaalId(id: Long): URI {
        return URI.create("${uriInfo.baseUri}communicatiekanalen/$id")
    }
}
