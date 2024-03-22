/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.zac.app.zaken.model.RESTCommunicatiekanaal
import net.atos.zac.util.UriUtil

fun convertToRESTCommunicatiekanaal(communicatieKanaal: CommunicatieKanaal): RESTCommunicatiekanaal =
    RESTCommunicatiekanaal(
        uuid = UriUtil.uuidFromURI(communicatieKanaal.url),
        naam = communicatieKanaal.naam
    )

fun convertToRESTCommunicatiekanalen(
    communicatieKanalen: List<CommunicatieKanaal>
): List<RESTCommunicatiekanaal> = communicatieKanalen.stream()
    .map { communicatieKanaal -> convertToRESTCommunicatiekanaal(communicatieKanaal) }
    .toList()
